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

package com.googlesource.gerrit.plugins.scripting.rules.engines.lua;

import com.google.common.collect.ImmutableList;
import com.google.gerrit.common.data.SubmitRecord;
import com.google.gerrit.common.data.SubmitRecord.Status;
import com.google.gerrit.common.data.SubmitRequirement;
import com.google.gerrit.reviewdb.client.Change;
import java.util.Collection;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

class GerritLib extends TwoArgFunction {

  private final Collection<SubmitRecord> result;
  private final Change change;

  GerritLib(Collection<SubmitRecord> result, Change change) {
    this.result = result;
    this.change = change;
  }

  /**
   * Perform one-time initialization on the library by creating a table containing the library
   * functions, adding that table to the supplied environment, adding the table to package.loaded,
   * and returning table as the return value. Creates a metatable that uses __INDEX to fall back on
   * itself to support string method operations. If the shared strings metatable instance is null,
   * will set the metatable as the global shared metatable for strings.
   *
   * <p>All tables and metatables are read-write by default so if this will be used in a server
   * environment, sandboxing should be used. In particular, the {@link LuaString#s_metatable} table
   * should probably be made read-only.
   *
   * @param modname the module name supplied if this is loaded via 'require'.
   * @param env the environment to load into, typically a Globals instance.
   */
  public LuaValue call(LuaValue modname, LuaValue env) {
    LuaTable string = new LuaTable();
    string.set("add_requirement", new AddRequirement(result));
    string.set("is_wip", LuaBoolean.valueOf(change.isWorkInProgress()));
    System.out.println("IsWIP" + change.isWorkInProgress());
    env.set("change", string);
    env.get("package").get("loaded").set("change", string);

    return env;
  }

  private static final class AddRequirement extends VarArgFunction {
    private final Collection<SubmitRecord> result;

    AddRequirement(Collection<SubmitRecord> result) {
      this.result = result;
    }

    public Varargs invoke(Varargs args) {
      boolean isReady = "ok".equalsIgnoreCase(args.checkstring(1).tojstring());
      LuaString fallbackText = args.checkstring(2);
      SubmitRecord sr = new SubmitRecord();
      sr.status = isReady ? SubmitRecord.Status.OK : Status.NOT_READY;
      sr.requirements =
          ImmutableList.of(
              SubmitRequirement.builder()
                  .setType("random")
                  .setFallbackText(fallbackText.tojstring())
                  .build());
      result.add(sr);
      System.out.println("Adding SR: " + sr);
      return varargsOf(new LuaValue[] {});
    }
  }
}
