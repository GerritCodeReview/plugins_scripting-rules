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

import com.google.gerrit.common.Nullable;
import com.google.gerrit.reviewdb.client.RefNames;
import com.google.gerrit.reviewdb.client.RevId;
import java.io.IOException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.RawParseUtils;

/**
 * Utility class to load several files from a repository, without opening it multiple times. See
 * {@link ThrowingSupplierTest} for examples.
 *
 * <p>This class is not thread-safe.
 */
public class FileFinder implements AutoCloseable {
  private RevCommit revision;
  private final ObjectReader reader;
  private final RevWalk walk;
  private final Repository git;

  public FileFinder(Repository git) {
    this.git = git;

    walk = new RevWalk(git);
    reader = walk.getObjectReader();
  }

  @Override
  public void close() {
    this.revision = null;
    walk.close();
    reader.close();
  }

  /** Returns the content of a file, at the current revision.e. */
  @Nullable
  public String readFile(String fileName) throws IOException {
    ObjectId objectId = findFile(fileName);
    if (objectId == null) {
      return null;
    }

    ObjectLoader obj = reader.open(objectId, Constants.OBJ_BLOB);
    byte[] raw = obj.getCachedBytes(Integer.MAX_VALUE);

    if (raw.length == 0) {
      return null;
    }
    return RawParseUtils.decode(raw);
  }

  /** Returns the object id for a given filename, at the current revision. */
  @Nullable
  public ObjectId findFile(String fileName) throws IOException {
    if (revision == null) {
      return null;
    }

    try (TreeWalk tw = TreeWalk.forPath(reader, fileName, revision.getTree())) {
      if (tw != null) {
        return tw.getObjectId(0);
      }
    }
    return null;
  }

  /** Places the pointer at refs/head/master's head. */
  public boolean pointAtMaster() {
    return pointAt(RefNames.fullName("master"));
  }

  /** Places the pointer at refs/meta/config's head. */
  public boolean pointAtMetaConfig() {
    return pointAt(RefNames.REFS_CONFIG);
  }

  /** Places the pointer at the specified's ref head. */
  public boolean pointAt(String refName) {
    revision = null;

    try {
      Ref ref = git.getRefDatabase().exactRef(refName);
      if (ref == null) {
        return false;
      }

      revision = walk.parseCommit(ref.getObjectId());
    } catch (IOException ignore) {
    }

    return revision != null;
  }

  private boolean pointAt(RevId revId) {
    revision = null;

    ObjectId id = ObjectId.fromString(revId.get());
    if (id == null) {
      return false;
    }

    try {
      revision = walk.parseCommit(id);
    } catch (IOException ignore) {
    }
    return revision != null;
  }
}
