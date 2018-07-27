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

package com.googlesource.gerrit.plugins.scripting.rules.utils;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.util.function.Supplier;
import org.junit.Test;

/**
 * Small tests to showcase the benefit of using the ThrowingSupplier class. Without it, the lambda
 * method is overly complex, and code needs to be duplicated (or extracted, for instance, in a class
 * named ThrowingSupplier).
 */
public class ThrowingSupplierTest {

  @Test
  public void demoUsage() {
    String message = doSomethingThrowingSupplier("Maxime", ThrowingSupplierTest::doWork);

    assertThat(message).isEqualTo("Hello, Maxime");
  }

  @Test
  public void sameDemoWithoutThrowingSupplier() {
    String message =
        doSomethingSupplier(
            "Maxime",
            () -> {
              try {
                return doWork();
              } catch (IOException e) {
                return "Unknown";
              }
            });

    assertThat(message).isEqualTo("Hello, Maxime");
  }

  private static String doSomethingThrowingSupplier(
      String name, ThrowingSupplier<String, IOException> method) {
    try {
      return method.get() + name;
    } catch (IOException e) {
      return "Unknown";
    }
  }

  private static String doSomethingSupplier(String name, Supplier<String> method) {
    return method.get() + name;
  }

  /** Simple method allowed to throw an exception */
  @SuppressWarnings("RedundantThrows")
  private static String doWork() throws IOException {
    return "Hello, ";
  }
}
