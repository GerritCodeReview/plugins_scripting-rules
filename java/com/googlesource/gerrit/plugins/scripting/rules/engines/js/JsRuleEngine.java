// Copyright (C) 2018 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.scripting.rules.engines.js;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8RuntimeException;
import com.google.common.collect.ImmutableList;
import com.google.gerrit.common.data.SubmitRecord;
import com.google.gerrit.common.data.SubmitRecord.Status;
import com.google.gerrit.common.data.SubmitRequirement;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSetApproval;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountState;
import com.google.gerrit.server.account.externalids.ExternalId;
import com.google.gerrit.server.project.RuleEvalException;
import com.google.gerrit.server.project.SubmitRuleOptions;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gwtorm.server.OrmException;
import com.googlesource.gerrit.plugins.scripting.rules.engines.RuleEngine;
import com.googlesource.gerrit.plugins.scripting.rules.utils.FileFinder;
import com.googlesource.gerrit.plugins.scripting.rules.utils.ThrowingSupplier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.eclipse.jgit.lib.PersonIdent;

class JsRuleEngine implements RuleEngine {
  private static final String SLOW_RULE = "Rule execution did not terminate in time";
  private static final long TIMEOUT_DELAY = 300;
  private final AccountCache accountCache;

  @Inject
  private JsRuleEngine(AccountCache accountCache) {
    this.accountCache = accountCache;
  }

  @Override
  public Collection<SubmitRecord> evaluate(
      ChangeData cd, Change change, SubmitRuleOptions opts, FileFinder fileFinder)
      throws IOException, OrmException, RuleEvalException {
    if (!fileFinder.pointAtMetaConfig()) {
      // The refs/meta/config branch does not exist
      return null;
    }

    String jsRules;
    try {
      jsRules = fileFinder.readFile("rules.js");
    } catch (IOException e) {
      throw new RuleEvalException("Could not read rules.js", e);
    }

    if (jsRules == null) {
      // The rules.js file does not exist
      return null;
    }

    try {
      return runScriptInSandbox(jsRules, change, cd);
    } catch (V8RuntimeException e) {
      SubmitRecord errorRecord = new SubmitRecord();
      errorRecord.status = Status.RULE_ERROR;
      errorRecord.requirements =
          ImmutableList.of(
              SubmitRequirement.builder()
                  .setFallbackText("Fix the rules.js file!")
                  .setType("rules_js_invalid")
                  .build());
      if (opts.logErrors() || true) {
        e.printStackTrace();
      }
      return ImmutableList.of(errorRecord);
    }
  }

  private Collection<SubmitRecord> runScriptInSandbox(String script, Change change, ChangeData cd)
      throws IOException, OrmException {
    V8 v8 = V8.createV8Runtime();
    try {
      final AtomicBoolean finished = new AtomicBoolean(false);

      // Setup the Requirement prototype
      v8.executeVoidScript(
          "function Requirement(is_met, description) {\n"
              + "this.is_met = is_met;\n"
              + "this.description = description;\n"
              + "};");
      startWatchdog(v8, finished);

      v8.executeScript(script, change.getProject().get() + ":/rules.js", 0);
      if (finished.get()) {
        throw new RuntimeException(SLOW_RULE);
      }

      V8Object v8Change = prepareChangeObject(v8, change, cd);
      V8Array v8Requirements = new V8Array(v8);

      try {
        v8.executeJSFunction("submit_rule", v8Change, v8Requirements);

        if (finished.getAndSet(true)) {
          throw new RuntimeException(SLOW_RULE);
        }

        if (v8Requirements.length() == 0) {
          // The script did not add any requirements.
          return null;
        }

        // We don't want to return records with zero requirements.
        return parseResults(v8Requirements)
            .stream()
            .filter(s -> !s.requirements.isEmpty())
            .collect(Collectors.toList());
      } finally {
        v8Change.release();
        v8Requirements.release();
      }
    } finally {
      v8.release();
    }
  }

  private Collection<SubmitRecord> parseResults(V8Array v8Requirements) {
    SubmitRecord okRequirements = new SubmitRecord();
    okRequirements.status = Status.OK;
    okRequirements.requirements = new ArrayList<>();

    SubmitRecord notReadyRequirements = new SubmitRecord();
    notReadyRequirements.status = Status.NOT_READY;
    notReadyRequirements.requirements = new ArrayList<>();

    for (int i = 0; i < v8Requirements.length(); i++) {
      V8Object v8Requirement = v8Requirements.getObject(i);
      SubmitRequirement requirement =
          SubmitRequirement.builder()
              .setFallbackText(v8Requirement.getString("description"))
              .setType("rules_js")
              .build();
      boolean isMet = v8Requirement.getBoolean("is_met");
      if (!isMet) {
        notReadyRequirements.requirements.add(requirement);
      } else {
        okRequirements.requirements.add(requirement);
      }
      v8Requirement.release();
    }
    return ImmutableList.of(okRequirements, notReadyRequirements);
  }

  private void startWatchdog(V8 v8, AtomicBoolean finished) {
    new Thread(
            () -> {
              try {
                Thread.sleep(TIMEOUT_DELAY);
              } catch (InterruptedException e) {
                return;
              }
              if (!finished.getAndSet(true)) {
                v8.terminateExecution();
              }
            })
        .start();
  }

  private V8Object prepareChangeObject(final V8 v8, Change change, ChangeData cd)
      throws IOException, OrmException {
    V8Object v8Change = new V8Object(v8);

    v8Change.registerJavaMethod(exposePersonIdent(v8, cd.getAuthor()), "author");
    v8Change.registerJavaMethod(exposePersonIdent(v8, cd.getCommitter()), "committer");

    defineProperty(v8Change, cd::unresolvedCommentCount, "unresolved_comments_count");
    defineProperty(v8Change, change::isPrivate, "private");
    defineProperty(v8Change, change::isWorkInProgress, "work_in_progress");
    defineProperty(v8Change, change::isWorkInProgress, "wip");
    defineProperty(v8Change, change::getSubject, "subject");
    defineProperty(v8Change, cd.currentPatchSet()::getRefName, "branch");

    v8Change.registerJavaMethod(findVotes(v8, cd.currentApprovals()), "findVotes");

    return v8Change;
  }

  private JavaCallback findVotes(V8 v8, List<PatchSetApproval> patchSetApprovals) {
    return (receiver, parameters) -> {
      String label = parameters.getString(0);
      Integer value = parameters.length() >= 2 ? parameters.getInteger(1) : null;
      V8Array v8Votes = new V8Array(v8);

      for (PatchSetApproval approval : patchSetApprovals) {
        if (!label.equalsIgnoreCase(approval.getLabel())) {
          continue;
        }
        if (value != null && value != approval.getValue()) {
          continue;
        }
        V8Object v8Author = new V8Object(v8);

        v8Author.add("label", approval.getLabel());
        v8Author.add("value", approval.getValue());
        v8Author.add("patchset_id", approval.getPatchSetId().patchSetId);

        V8Object v8Account = new V8Object(v8);
        v8Author.add("account", v8Account);
        AccountState account = accountCache.getEvenIfMissing(approval.getAccountId());

        v8Account.registerJavaMethod(
            new JavaCallback() {
              @Override
              public Object invoke(V8Object receiver, V8Array parameters) {
                try {
                  String emailToCheck = parameters.getString(0);
                  for (ExternalId extId : account.getExternalIds()) {
                    if (emailToCheck.equalsIgnoreCase(extId.email())) {
                      return true;
                    }
                  }
                  return false;
                } catch (Exception e) {
                  return null;
                }
              }
            },
            "hasEmail");

        v8Votes.push(v8Author);
        v8Author.release();
        v8Account.release();
      }

      return v8Votes;
    };
  }

  private void defineProperty(
      V8Object myObject, ThrowingSupplier<?, ?> supplier, String methodName) {
    V8 v8 = myObject.getRuntime();
    V8Object methodProperty = new V8Object(v8);
    methodProperty.registerJavaMethod(
        new JavaCallback() {
          @Override
          public Object invoke(V8Object receiver, V8Array parameters) {
            try {
              return supplier.get();
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        },
        "get");

    V8Object object = v8.getObject("Object");
    V8Object ret =
        (V8Object) object.executeJSFunction("defineProperty", myObject, methodName, methodProperty);

    object.release();
    ret.release();
    methodProperty.release();
  }

  private JavaCallback exposePersonIdent(V8 v8, PersonIdent personIdentSupplier) {
    return (receiver, parameters) -> {
      PersonIdent author;
      author = personIdentSupplier;
      V8Object v8Author = new V8Object(v8);
      v8Author.add("email", author.getEmailAddress());
      v8Author.add("name", author.getName());
      return v8Author;
    };
  }
}
