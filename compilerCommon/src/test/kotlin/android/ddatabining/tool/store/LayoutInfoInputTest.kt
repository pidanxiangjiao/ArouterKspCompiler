/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.databinding.tool.store

import android.databinding.tool.DataBindingBuilder
import org.apache.commons.io.FileUtils
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LayoutInfoInputTest {
    @Rule
    @JvmField
    val tmpFolder = TemporaryFolder()
    lateinit var infoFolder: File
    lateinit var dependencyClassFolder: File
    lateinit var baseBinderLogFolder: File
    lateinit var artifactFolder: File
    @Before
    fun setup() {
        infoFolder = tmpFolder.newFolder("info")
        dependencyClassFolder = tmpFolder.newFolder("dep-classes")
        baseBinderLogFolder = tmpFolder.newFolder("binder-log")
        artifactFolder = tmpFolder.newFolder("out-bundle-folder")
    }

    @Test
    fun empty() {
        val input = create(
                added = emptyList(),
                removed = emptyList(),
                incremental = false,
                log = null)
        assertThat(input.invalidatedClasses, `is`(emptySet()))
        assertThat(input.existingBindingClasses.mappings(), `is`(emptyMap()))
        assertThat(input.filesToConsider, `is`(setOf()))
        assertThat(input.unchangedLog.classInfoLog.mappings(), `is`(emptyMap()))
    }

    @Test
    fun fresh() {
        val foo = createInfoFile("foo")
        val bar = createInfoFile("bar")
        val input = create(
                added = emptyList(),
                removed = emptyList(),
                incremental = false,
                log = null)
        assertThat(input.invalidatedClasses, `is`(emptySet()))
        assertThat(input.existingBindingClasses.mappings(), `is`(emptyMap()))
        assertThat(input.filesToConsider, `is`(setOf(foo, bar)))
        assertThat(input.unchangedLog.classInfoLog.mappings(), `is`(emptyMap()))
    }

    @Test
    fun increment_add() {
        val foo = createInfoFile("foo")
        val bar = createInfoFile("bar")
        val log = LayoutInfoLog()
        val fooClass = createClass("com.Foo")
        val barClass = createClass("com.Bar")

        log.classInfoLog.addMapping("foo", fooClass)
        log.classInfoLog.addMapping("bar", barClass)
        val added = createInfoFile("baz")
        val input = create(
                added = listOf(added),
                removed = emptyList(),
                incremental = true,
                log = log)

        val prevMapping = mapOf(
                "foo" to fooClass,
                "bar" to barClass
        )
        assertThat(input.invalidatedClasses, `is`(emptySet()))
        assertThat(input.existingBindingClasses.mappings(), `is`(prevMapping))
        assertThat(input.filesToConsider, `is`(setOf(added)))
        assertThat(input.unchangedLog.classInfoLog.mappings(), `is`(prevMapping))
    }

    @Test
    fun increment_delete() {
        val foo = createInfoFile("foo")
        val bar = createInfoFile("bar")
        val log = LayoutInfoLog()
        val fooClass = createClass("com.Foo")
        val barClass = createClass("com.Bar")


        log.classInfoLog.addMapping("foo", fooClass)
        log.classInfoLog.addMapping("bar", barClass)
        bar.delete()
        val input = create(
                added = emptyList(),
                removed = listOf(bar),
                incremental = true,
                log = log)
        val prevMapping = mapOf(
                "foo" to fooClass
        )
        assertThat(input.invalidatedClasses, `is`(setOf("com.Bar")))
        assertThat(input.existingBindingClasses.mappings(), `is`(prevMapping))
        assertThat(input.filesToConsider, `is`(emptySet()))
        assertThat(input.unchangedLog.classInfoLog.mappings(), `is`(prevMapping))
    }

    @Test
    fun increment_addConfigLayout() {
        val foo = createInfoFile("foo")
        val bar = createInfoFile("bar")
        val log = LayoutInfoLog()
        val fooClass = createClass("com.Foo")
        val barClass = createClass("com.Bar")
        log.classInfoLog.addMapping("foo", fooClass)
        log.classInfoLog.addMapping("bar", barClass)
        val fooLand = createInfoFile("foo", "land")
        val input = create(
                added = listOf(fooLand),
                removed = emptyList(),
                incremental = true,
                log = log)

        val prevMapping = mapOf(
                "bar" to barClass
        )
        assertThat(input.invalidatedClasses, `is`(setOf("com.Foo")))
        assertThat(input.existingBindingClasses.mappings(), `is`(prevMapping))
        assertThat(input.filesToConsider, `is`(setOf(foo, fooLand)))
        assertThat(input.unchangedLog.classInfoLog.mappings(), `is`(prevMapping))
    }

    @Test
    fun increment_removeConfigLayout() {
        val foo = createInfoFile("foo")
        val bar = createInfoFile("bar")
        val fooLand = createInfoFile("foo", "land")
        val log = LayoutInfoLog()
        val fooClass = createClass("com.Foo")
        val barClass = createClass("com.Bar")

        log.classInfoLog.addMapping("foo", fooClass)
        log.classInfoLog.addMapping("bar", barClass)
        fooLand.delete()
        val input = create(
                added = emptyList(),
                removed = listOf(fooLand),
                incremental = true,
                log = log)

        val prevMapping = mapOf(
                "bar" to barClass
        )
        assertThat(input.invalidatedClasses, `is`(setOf("com.Foo")))
        assertThat(input.existingBindingClasses.mappings(), `is`(prevMapping))
        assertThat(input.filesToConsider, `is`(setOf(foo)))
        assertThat(input.unchangedLog.classInfoLog.mappings(), `is`(prevMapping))
    }

    @Test
    fun increment_dependency() {
        val foo = createInfoFile("foo")
        val fooLand = createInfoFile("foo", "land")
        val bar = createInfoFile("bar")
        val baz = createInfoFile("baz")
        val fooClass = createClass("com.Foo")
        val barClass = createClass("com.Bar")
        val bazClass = createClass("com.Baz")

        val log = LayoutInfoLog()
        log.classInfoLog.addMapping("foo", fooClass)
        log.classInfoLog.addMapping("bar", barClass)
        log.classInfoLog.addMapping("baz", bazClass)
        // foo depends on baz
        log.addDependency("foo", "baz")

        // update baz
        FileUtils.touch(baz)
        val input = create(
                added = listOf(baz),
                removed = emptyList(),
                incremental = true,
                log = log
        )
        val prevMapping = mapOf(
                "bar" to barClass
        )

        assertThat(input.invalidatedClasses, `is`(setOf("com.Foo", "com.Baz")))
        assertThat(input.existingBindingClasses.mappings(), `is`(prevMapping))
        assertThat(input.filesToConsider, `is`(setOf(foo, fooLand, baz)))
        assertThat(input.unchangedLog.classInfoLog.mappings(), `is`(prevMapping))
    }

    @Test
    fun increment_external_dependency_newDependency() {
        val foo = createInfoFile("foo")
        val bar = createInfoFile("bar")
        val fooClass = createClass("com.Foo")
        val barClass = createClass("com.Bar")
        val libClassInfoLog = GenClassInfoLog()
        val bazClass = GenClassInfoLog.GenClass(
                qName = "com.Baz",
                variables = mapOf(
                        "var1" to "Int",
                        "var2" to "String"),
                modulePackage = "y.x",
                implementations = setOf(
                        GenClassInfoLog.GenClassImpl(
                                tag = "foo",
                                merge = false,
                                qualifiedName = "com.Baz.Impl"
                        )
                ))
        libClassInfoLog.addMapping("baz", bazClass)
        val libInfo = File(dependencyClassFolder,
                "com.baz." + _root_ide_package_.android.databinding.tool.DataBindingBuilder.BINDING_CLASS_LIST_SUFFIX)
        libClassInfoLog.serialize(libInfo)

        val log = LayoutInfoLog()
        log.classInfoLog.addMapping("foo", fooClass)
        log.classInfoLog.addMapping("bar", barClass)
        // add dependency from external lib
        log.addDependency("foo", "baz")

        val input = create(
                added = emptyList(),
                removed = emptyList(),
                incremental = true,
                log = log
        )

        val prevMapping = mapOf(
                "bar" to barClass
        )

        assertThat(input.invalidatedClasses, `is`(setOf("com.Foo")))
        assertThat(input.existingBindingClasses.mappings(), `is`(mapOf(
                "bar" to barClass,
                "baz" to bazClass
        )))
        assertThat(input.filesToConsider, `is`(setOf(foo)))
        assertThat(input.unchangedLog.classInfoLog.mappings(), `is`(prevMapping))
    }

    @Test
    fun increment_external_dependency_unrelatedChange() {
        val foo = createInfoFile("foo")
        val bar = createInfoFile("bar")
        val fooClass = createClass("com.Foo")
        val barClass = createClass("com.Bar")
        val bazClass = GenClassInfoLog.GenClass(
                qName = "com.Baz",
                variables = mapOf(
                        "var1" to "Int",
                        "var2" to "String"),
                modulePackage = "x.y",
                implementations = setOf(GenClassInfoLog.GenClassImpl(
                        tag = "foo",
                        merge = false,
                        qualifiedName = "com.Baz.Impl"
                )))
        val libClassInfoLog = GenClassInfoLog()
        libClassInfoLog.addMapping("baz", bazClass)
        val libInfo = File(dependencyClassFolder,
                "com.baz." + _root_ide_package_.android.databinding.tool.DataBindingBuilder.BINDING_CLASS_LIST_SUFFIX)
        libClassInfoLog.serialize(libInfo)

        val log = LayoutInfoLog()
        log.classInfoLog.addMapping("foo", fooClass)
        log.classInfoLog.addMapping("bar", barClass)

        val input = create(
                added = emptyList(),
                removed = emptyList(),
                incremental = true,
                log = log
        )

        val prevMapping = mapOf(
                "bar" to barClass,
                "foo" to fooClass
        )

        assertThat(input.invalidatedClasses, `is`(emptySet()))
        assertThat(input.existingBindingClasses.mappings(), `is`(mapOf(
                "bar" to barClass,
                "foo" to fooClass,
                "baz" to bazClass
        )))
        assertThat(input.filesToConsider, `is`(emptySet()))
        assertThat(input.unchangedLog.classInfoLog.mappings(), `is`(prevMapping))
    }

    private fun createInfoFile(name: String, config: String = ""): File {
        val configSuffix = if (config == "") {
            ""
        } else {
            "-$config"
        }
        val file = File(infoFolder, "$name-layout$configSuffix.xml")
        FileUtils.touch(file)
        return file
    }

    private fun createClass(qName: String, variables: Map<String, String> = emptyMap())
            : GenClassInfoLog.GenClass {
        return GenClassInfoLog.GenClass(qName = qName,
                variables = variables,
                modulePackage = "android.x",
                implementations = setOf(
                        GenClassInfoLog.GenClassImpl(
                                tag = "foo",
                                merge = false,
                                qualifiedName = "${qName}Impl")))
    }


    private fun create(
            added: List<File>,
            removed: List<File>,
            incremental: Boolean,
            log: LayoutInfoLog?): LayoutInfoInput {
        log?.serialize(File(baseBinderLogFolder, LayoutInfoInput.LOG_FILE_NAME))
        return LayoutInfoInput(
                LayoutInfoInput.Args(
                        outOfDate = added,
                        removed = removed,
                        infoFolder = infoFolder,
                        dependencyClassesFolder = dependencyClassFolder,
                        incremental = incremental,
                        logFolder = baseBinderLogFolder,
                        artifactFolder = artifactFolder,
                        packageName = "foo.bar.baz",
                        useAndroidX = true,
                        enableViewBinding = false,
                        enableDataBinding = true)
        )
    }
}
