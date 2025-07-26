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

package android.databinding.tool.writer

import android.databinding.tool.LayoutResourceRule
import android.databinding.tool.LibTypes
import android.databinding.tool.assert
import org.junit.Rule
import org.junit.Test

class BaseLayoutBinderWriterTest {
    @get:Rule val layouts = LayoutResourceRule(dataBindingEnabled = true)

    @Test fun nullableFieldsJavadocTheirConfigurations() {
        layouts.write("example", "layout", """
            <layout xmlns:android="http://schemas.android.com/apk/res/android">
                <LinearLayout>
                    <TextView android:id="@+id/name" />
                </LinearLayout>
            </layout>
            """.trimIndent())

        layouts.write("example", "layout-sw600dp", """
            <layout xmlns:android="http://schemas.android.com/apk/res/android">
                <LinearLayout>
                    <TextView android:id="@+id/name" />
                </LinearLayout>
            </layout>
            """.trimIndent())

        layouts.write("example", "layout-land", """
            <layout xmlns:android="http://schemas.android.com/apk/res/android">
                <LinearLayout/>
            </layout>
            """.trimIndent())

        val model = layouts.parse().getValue("example")
        val writer = BaseLayoutBinderWriter(model, LibTypes(true))
        writer.write().assert {
            contains("""
                |  /**
                |   * This binding is not available in all configurations.
                |   * <p>
                |   * Present:
                |   * <ul>
                |   *   <li>layout/</li>
                |   *   <li>layout-sw600dp/</li>
                |   * </ul>
                |   *
                |   * Absent:
                |   * <ul>
                |   *   <li>layout-land/</li>
                |   * </ul>
                |   */
                |  @Nullable
                |  public final TextView name;
                """.trimMargin())
        }
    }
}
