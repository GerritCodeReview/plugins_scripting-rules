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

package com.googlesource.gerrit.plugins.scripting.rules.engines;

import com.google.gerrit.common.Nullable;
import com.google.gerrit.common.data.SubmitRecord;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.server.project.RuleEvalException;
import com.google.gerrit.server.project.SubmitRuleOptions;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gwtorm.server.OrmException;
import com.googlesource.gerrit.plugins.scripting.rules.utils.FileFinder;
import java.io.IOException;
import java.util.Collection;

/** Defines a scripting engine, called when a change must be evaluated. */
public interface RuleEngine {
  @Nullable
  Collection<SubmitRecord> evaluate(
      ChangeData cd, Change change, SubmitRuleOptions opts, FileFinder fileFinder)
      throws IOException, OrmException, RuleEvalException;
}
