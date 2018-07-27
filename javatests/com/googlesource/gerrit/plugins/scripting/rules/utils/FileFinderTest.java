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
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileFinderTest {
  private Repository git;
  private TestRepository<Repository> repo;

  @Before
  public void setUp() throws Exception {
    git = new InMemoryRepository(new DfsRepositoryDescription("test_repo"));
    repo = new TestRepository<>(git);
  }

  @After
  public void tearDown() {
    git.close();
  }

  @Test
  public void readFile() throws Exception {
    repo.update("master", repo.commit().add("existant-file", "content"));

    try (FileFinder fileFinder = new FileFinder(git)) {
      boolean pointingWorked = fileFinder.pointAtMaster();
      assertThat(pointingWorked).isTrue();

      String content = fileFinder.readFile("existant-file");
      assertThat(content).isEqualTo("content");
    }
  }

  @Test
  public void findsFileInMaster() throws Exception {
    repo.update("master", repo.commit().add("existant-file", "content"));

    try (FileFinder fileFinder = new FileFinder(git)) {
      boolean pointingWorked = fileFinder.pointAtMaster();
      assertThat(pointingWorked).isTrue();

      ObjectId objectId = fileFinder.findFile("inexistant-file");
      assertThat(objectId).isNull();

      ObjectId existingId = fileFinder.findFile("existant-file");
      assertThat(existingId).isNotNull();
    }
  }

  @Test
  public void pointAtMasterFailsWhenMasterBranchDoesNotExist() throws IOException {
    try (FileFinder fileFinder = new FileFinder(git)) {
      boolean pointingWorked = fileFinder.pointAtMaster();
      assertThat(pointingWorked).isFalse();
    }
  }

  @Test
  public void pointAtWorksWhenBranchExists() throws Exception {
    repo.update("master", repo.commit().add("existant-file", "content"));
    try (FileFinder fileFinder = new FileFinder(git)) {
      boolean pointingWorked = fileFinder.pointAt("refs/heads/master");
      assertThat(pointingWorked).isTrue();
    }
  }

  @Test
  public void pointAtDoesIsNotConfusedByCommonPrefix() throws Exception {
    repo.update("master/nope", repo.commit().add("existant-file", "content"));
    try (FileFinder fileFinder = new FileFinder(git)) {
      boolean pointingWorked = fileFinder.pointAt("refs/heads/master");
      assertThat(pointingWorked).isFalse();
    }
  }

  @Test
  public void pointAtCanBeCalledAfterAFailure() throws Exception {
    repo.update("master", repo.commit().add("existant-file", "content"));
    boolean pointingWorked;

    try (FileFinder fileFinder = new FileFinder(git)) {
      pointingWorked = fileFinder.pointAtMaster();
      assertThat(pointingWorked).isTrue();

      pointingWorked = fileFinder.pointAtMetaConfig();
      assertThat(pointingWorked).isFalse();

      pointingWorked = fileFinder.pointAtMaster();
      assertThat(pointingWorked).isTrue();
    }
  }
}
