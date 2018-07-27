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

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

// Simple read-only table whose contents are initialized from another table.
class ImmutableLuaTable extends LuaTable {
  ImmutableLuaTable(LuaValue table) {
    presize(table.length(), 0);
    for (Varargs n = table.next(LuaValue.NIL); !n.arg1().isnil(); n = table.next(n.arg1())) {
      LuaValue key = n.arg1();
      LuaValue value = n.arg(2);
      super.rawset(key, value.istable() ? new ImmutableLuaTable(value) : value);
    }
  }

  public LuaValue setmetatable(LuaValue metatable) {
    return error("table is read-only");
  }

  public void set(int key, LuaValue value) {
    error("table is read-only");
  }

  public void rawset(int key, LuaValue value) {
    error("table is read-only");
  }

  public void rawset(LuaValue key, LuaValue value) {
    error("table is read-only");
  }

  public LuaValue remove(int pos) {
    return error("table is read-only");
  }
}
