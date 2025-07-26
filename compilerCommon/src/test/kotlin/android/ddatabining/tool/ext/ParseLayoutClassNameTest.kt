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

package android.databinding.tool.ext

import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import org.junit.Test

class ParseLayoutClassNameTest {
    @Test fun simple() {
        assertThat(parseLayoutClassName("com.example.Simple", "foo")).isEqualTo(
            ClassName.get("com.example", "Simple"))
    }

    @Test fun nesting() {
        assertThat(parseLayoutClassName("com.example.Simple\$Nested", "foo")).isEqualTo(
            ClassName.get("com.example", "Simple", "Nested"))
        assertThat(parseLayoutClassName("com.example.Simple\$Nested\$Very\$Deeply", "foo")).isEqualTo(
            ClassName.get("com.example", "Simple", "Nested", "Very", "Deeply"))
    }

    @Test fun defaultPackage() {
        assertThat(parseLayoutClassName("DefaultPackage", "foo")).isEqualTo(
            ClassName.get("", "DefaultPackage"))
        assertThat(parseLayoutClassName("DefaultPackage\$Nested", "foo")).isEqualTo(
            ClassName.get("", "DefaultPackage", "Nested"))
    }

    @Test fun weirdCasing() {
        assertThat(parseLayoutClassName("com.Titlecase.UPPERCASE.camelCase.Simple", "foo")).isEqualTo(
            ClassName.get("com.Titlecase.UPPERCASE.camelCase", "Simple"))
        assertThat(parseLayoutClassName("com.Titlecase.UPPERCASE.camelCase.Simple\$lowercase\$UPPERCASE\$camelCase", "foo")).isEqualTo(
            ClassName.get("com.Titlecase.UPPERCASE.camelCase", "Simple", "lowercase", "UPPERCASE", "camelCase"))
    }

    @Test fun failureThrows() {
        try {
            parseLayoutClassName("layer-list", "foo")
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessageThat()
                .isEqualTo("Unable to parse \"layer-list\" as class in foo.xml")
        }
    }
}
