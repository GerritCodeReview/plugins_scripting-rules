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

import com.google.gerrit.common.data.SubmitRecord;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.server.project.SubmitRuleOptions;
import com.google.gerrit.server.query.change.ChangeData;
import com.googlesource.gerrit.plugins.scripting.rules.engines.RuleEngine;
import com.googlesource.gerrit.plugins.scripting.rules.utils.FileFinder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaThread;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JseMathLib;

public class LuaRuleEngine implements RuleEngine {
  private static final int MAX_INSTRUCTIONS_COUNT = 50;
  // Source: http://www.luaj.org/luaj/3.0/examples/jse/SampleSandboxed.java
  // These globals are used by the server to compile scripts.
  private final Globals server_globals;

  public LuaRuleEngine() {
    // Create server globals with just enough library support to compile user scripts.
    server_globals = new Globals();
    server_globals.load(new SandboxedBaseLib());
    server_globals.load(new PackageLib());
    server_globals.load(new StringLib());

    // To load scripts, we occasionally need a math library in addition to compiler support.
    // To limit scripts using the debug library, they must be closures, so we only install LuaC.
    server_globals.load(new JseMathLib());
    LoadState.install(server_globals);
    LuaC.install(server_globals);

    // Set up the LuaString metatable to be read-only since it is shared across all scripts.
    LuaString.s_metatable = new ImmutableLuaTable(LuaString.s_metatable);
  }

  // Run a script in a lua thread and limit it to a certain number
  // of instructions by setting a hook function.
  // Give each script its own copy of globals, but leave out libraries
  // that contain functions that can be abused.
  public Collection<SubmitRecord> runScriptInSandbox(String script, Change change) {
    Collection<SubmitRecord> results = new ArrayList<>();
    System.out.println("Script: " + script);
    // Each script will have it's own set of globals, which should
    // prevent leakage between scripts running on the same server.
    Globals user_globals = new Globals();
    user_globals.load(new SandboxedBaseLib());
    user_globals.load(new PackageLib());
    user_globals.load(new Bit32Lib());
    user_globals.load(new TableLib());
    user_globals.load(new StringLib());
    user_globals.load(new JseMathLib());

    // This library is dangerous as it gives unfettered access to the
    // entire Java VM, so it's not suitable within this lightweight sandbox.
    // user_globals.load(new LuajavaLib());

    // Starting coroutines in scripts will result in threads that are
    // not under the server control, so this libary should probably remain out.
    // user_globals.load(new CoroutineLib());

    // These are probably unwise and unnecessary for scripts on servers,
    // although some date and time functions may be useful.
    // user_globals.load(new JseIoLib());
    // user_globals.load(new JseOsLib());

    // Loading and compiling scripts from within scripts may also be
    // prohibited, though in theory it should be fairly safe.
    // LoadState.install(user_globals);
    // LuaC.install(user_globals);

    // The debug library must be loaded for hook functions to work, which
    // allow us to limit scripts to run a certain number of instructions at a time.
    // However we don't wish to expose the library in the user globals,
    // so it is immediately removed from the user globals once created.
    user_globals.load(new DebugLib());
    LuaValue sethook = user_globals.get("debug").get("sethook");
    user_globals.set("debug", LuaValue.NIL);

    // Gerrit specific code
    user_globals.load(new GerritLib(results, change));
    // End of gerrit code

    // Set up the script to run in its own lua thread, which allows us
    // to set a hook function that limits the script to a specific number of cycles.
    // Note that the environment is set to the user globals, even though the
    // compiling is done with the server globals.
    LuaValue chunk = server_globals.load(script, "main", user_globals);
    LuaThread thread = new LuaThread(user_globals, chunk);

    // Set the hook function to immediately throw an Error, which will not be
    // handled by any Lua code other than the coroutine.
    LuaValue hookfunc =
        new ZeroArgFunction() {
          public LuaValue call() {
            // A simple lua error may be caught by the script, but a
            // Java Error will pass through to top and stop the script.
            throw new Error("Script overran resource limits.");
          }
        };

    sethook.invoke(
        LuaValue.varargsOf(
            new LuaValue[] {
              thread, hookfunc, LuaValue.EMPTYSTRING, LuaValue.valueOf(MAX_INSTRUCTIONS_COUNT)
            }));

    // When we resume the thread, it will run up to 'instruction_count' instructions
    // then call the hook function which will error out and stop the script.
    Varargs result = thread.resume(LuaValue.NIL);
    System.out.println("[[" + script + "]] -> " + result);
    return results;
  }

  @Override
  public Collection<SubmitRecord> evaluate(
      ChangeData cd, Change change, SubmitRuleOptions opts, FileFinder fileFinder) {
    if (!fileFinder.pointAtMetaConfig()) {
      return null;
    }

    String luaRules;
    try {
      luaRules = fileFinder.readFile("rules.lua");

    } catch (IOException e) {
      // throw new RuleEvalException("Could not read rules.lua", e);
      return null;
    }

    if (luaRules == null) {
      return null;
    }

    return runScriptInSandbox(luaRules, change);
  }
}
