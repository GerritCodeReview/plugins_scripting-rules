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

import java.io.InputStream;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JseBaseLib;

class SandboxedBaseLib extends JseBaseLib {

  @Override
  public LuaValue call(LuaValue var1, LuaValue var2) {
    super.call(var1, var2);

    // Unneeded informations
    unset(var2, "_G");
    unset(var2, "_VERSION");

    // Scripts shouldn't have to collect garbage manually
    unset(var2, "collectgarbage");

    // IO related functions
    unset(var2, "dofile");
    unset(var2, "loadfile");
    unset(var2, "print");

    // Disable meta-programming capabilities
    // load: "Loads a chunk by calling a function repeatedly"
    unset(var2, "load");

    // Metadata table related functions
    unset(var2, "setmetatable");
    unset(var2, "getmetatable");
    unset(var2, "rawequal");
    unset(var2, "rawget");
    unset(var2, "rawlen");
    unset(var2, "rawset");

    return var2;
  }

  private static void unset(LuaValue var2, String key) {
    var2.set(key, LuaValue.NIL);
  }

  @Override
  public InputStream findResource(String s) {
    return null;
  }
}
