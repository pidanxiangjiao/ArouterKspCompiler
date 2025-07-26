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

package android.databinding.tool

import android.databinding.tool.store.LayoutFileParser
import android.databinding.tool.store.ResourceBundle
import android.databinding.tool.store.ResourceBundle.LayoutFileBundle
import android.databinding.tool.util.RelativizableFile
import android.databinding.tool.writer.BaseLayoutModel
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File

class LayoutResourceRule(
    private val appPackage: String = "com.example",
    private val useAndroidX: Boolean = true,
    private val viewBindingEnabled: Boolean = false,
    private val dataBindingEnabled: Boolean = false
) : TestRule {
    private val temporaryFolder = object : TemporaryFolder() {
        override fun before() {
            super.before()
            realResDir = newFolder("res")
            strippedResDir = newFolder("res-stripped")
        }
    }

    private lateinit var realResDir: File
    private lateinit var strippedResDir: File

    override fun apply(base: Statement, description: Description): Statement {
        return temporaryFolder.apply(base, description)
    }

    fun write(name: String, folder: String, content: String) {
        require(folder == "layout" || folder.startsWith("layout-"))
        require(!name.endsWith(".xml"))

        val folderDir = File(realResDir, folder)
        folderDir.mkdir()
        val layoutFile = File(folderDir, "$name.xml")
        layoutFile.writeText(content)
    }

    fun parse(): Map<String, BaseLayoutModel> {
        val resourceBundle = _root_ide_package_.android.databinding.tool.store.ResourceBundle(
            appPackage,
            useAndroidX
        )
        realResDir.walkTopDown().filter { it.isFile }.forEach { file ->
            val strippedFile = File(strippedResDir, file.toRelativeString(realResDir))
            val bundle = _root_ide_package_.android.databinding.tool.store.LayoutFileParser.parseXml(
                RelativizableFile.fromAbsoluteFile(file),
                strippedFile,
                appPackage,
                { null },
                viewBindingEnabled,
                dataBindingEnabled
            )
            if (bundle != null) {
                resourceBundle.addLayoutBundle(bundle, true)
            }
        }
        resourceBundle.validateAndRegisterErrors()

        return resourceBundle.allLayoutFileBundlesInSource
            .groupBy(LayoutFileBundle::getFileName)
            .mapValues { BaseLayoutModel(it.value) }
    }
}
