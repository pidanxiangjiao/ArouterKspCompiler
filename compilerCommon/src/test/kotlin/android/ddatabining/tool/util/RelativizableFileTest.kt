/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.databinding.tool.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class RelativizableFileTest {

  @Test
  fun testFromRelativeFile() {
    val file = RelativizableFile.fromRelativeFile(File(path("/a")), File(path("b/c")))
    assertEquals(path("/a/b/c"), file.absoluteFile.path)
    assertEquals(path("/a"), file.baseDir!!.path)
    assertEquals(path("b/c"), file.relativeFile!!.path)

    try {
      RelativizableFile.fromRelativeFile(File(path("a")), File(path("b/c")))
      fail("Expected IllegalStateException")
    } catch (e: IllegalStateException) {
      assertEquals("${path("a")} is not an absolute path", e.message)
    }

    try {
      RelativizableFile.fromRelativeFile(File(path("/a")), File(path("/b/c")))
      fail("Expected IllegalStateException")
    } catch (e: IllegalStateException) {
      assertEquals("${path("/b/c")} is not a relative path", e.message)
    }
  }

  @Test
  fun testFromAbsoluteFile() {
    val file = RelativizableFile.fromAbsoluteFile(File(path("/a/b/c")))
    assertEquals(path("/a/b/c"), file.absoluteFile.path)
    assertNull(file.baseDir)
    assertNull(file.relativeFile)

    try {
      RelativizableFile.fromAbsoluteFile(File(path("a/b/c")))
      fail("Expected IllegalStateException")
    } catch (e: IllegalStateException) {
      assertEquals("${path("a/b/c")} is not an absolute path", e.message)
    }
  }

  @Test
  fun testFromAbsoluteFileWithBaseDir() {
    run {
      val file = RelativizableFile.fromAbsoluteFile(File(path("/a/b/c")), File(path("/a")))
      assertEquals(path("/a/b/c"), file.absoluteFile.path)
      assertEquals(path("/a"), file.baseDir!!.path)
      assertEquals(path("b/c"), file.relativeFile!!.path)
    }

    try {
      RelativizableFile.fromAbsoluteFile(File(path("a/b/c")), File(path("/a")))
      fail("Expected IllegalStateException")
    } catch (e: java.lang.IllegalStateException) {
      assertEquals("${path("a/b/c")} is not an absolute path", e.message)
    }

    try {
      RelativizableFile.fromAbsoluteFile(File(path("/a/b/c")), File(path("a")))
      fail("Expected IllegalStateException")
    } catch (e: java.lang.IllegalStateException) {
      assertEquals("${path("a")} is not an absolute path", e.message)
    }

    run {
      // When absoluteFile is located outside of baseDir, baseDir is ignored.
      val file = RelativizableFile.fromAbsoluteFile(
          absoluteFile = File(path("/a/b/c")), baseDir = File(path("/x")))
      assertEquals(path("/a/b/c"), file.absoluteFile.path)
      assertNull(file.baseDir)
      assertNull(file.relativeFile)
    }
  }

  /**
   * Converts a Unix path containing forward slashes '/' to a path that is suitable for the
   * current filesystem.
   */
  private fun path(unixPath: String): String {
    return if (File.separatorChar != '/') {
      if (unixPath.startsWith("/")) {
        // Handle the root first (convert "/" to "X:\")
        unixPath.replaceFirst("/", root).replace('/', File.separatorChar)
      } else {
        unixPath.replace('/', File.separatorChar)
      }
    } else {
      unixPath
    }
  }

  companion object {

    /**
     * The path of the filesystem root (e.g., "X:\" on Windows).
     */
    private val root: String

    init {
      var file = File(".").absoluteFile
      while (file.parentFile != null) {
        file = file.parentFile
      }
      root = file.path
    }
  }
}