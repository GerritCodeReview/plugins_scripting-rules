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

package com.googlesource.gerrit.plugins.scripting.rules.rule;

import com.google.gerrit.common.data.SubmitRecord;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.project.RuleEvalException;
import com.google.gerrit.server.project.SubmitRuleEvaluator;
import com.google.gerrit.server.project.SubmitRuleOptions;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.rules.SubmitRule;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.scripting.rules.engines.RuleEngine;
import com.googlesource.gerrit.plugins.scripting.rules.utils.FileFinder;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.eclipse.jgit.lib.Repository;

/** This SubmitRule runs the scripting engines it knows about. */
@Singleton
public class ScriptedRule implements SubmitRule {
  private final GitRepositoryManager gitMgr;
  private final DynamicSet<RuleEngine> engines;

  @Inject
  private ScriptedRule(GitRepositoryManager gitMgr, DynamicSet<RuleEngine> engines) {
    this.gitMgr = gitMgr;
    this.engines = engines;
  }

  @Override
  public Collection<SubmitRecord> evaluate(ChangeData cd, SubmitRuleOptions options) {
    try (Repository git = gitMgr.openRepository(cd.project());
        FileFinder fileFinder = new FileFinder(git)) {

      Change change = cd.change();

      return StreamSupport.stream(engines.spliterator(), false)
          .map(new ScriptEvaluator(cd, change, options, fileFinder))
          .filter(Objects::nonNull)
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
    } catch (OrmException | IOException e) {
      e.printStackTrace();
      return SubmitRuleEvaluator.createRuleError("Error in ScriptedRule");
    }
  }

  /** Helper class to evaluate a scripting engine and catching its potential exceptions. */
  private class ScriptEvaluator implements Function<RuleEngine, Collection<SubmitRecord>> {
    private final ChangeData cd;
    private final Change change;
    private final SubmitRuleOptions options;
    private final FileFinder fileFinder;

    private ScriptEvaluator(
        ChangeData cd, Change change, SubmitRuleOptions options, FileFinder fileFinder) {

      this.cd = cd;
      this.change = change;
      this.options = options;
      this.fileFinder = fileFinder;
    }

    @Override
    public Collection<SubmitRecord> apply(RuleEngine ruleEngine) {
      try {
        return ruleEngine.evaluate(cd, change, options, fileFinder);
      } catch (IOException | OrmException | RuleEvalException e) {
        return SubmitRuleEvaluator.createRuleError("Error evaluating the rules");
      }
    }
  }
}
