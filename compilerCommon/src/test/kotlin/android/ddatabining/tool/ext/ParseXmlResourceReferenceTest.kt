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

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ParseXmlResourceReferenceTest {
    @Test fun xmlResourceReferenceParsing() {
        assertEquals(
            XmlResourceReference(null, "id", "foo", false),
            "@id/foo".parseXmlResourceReference()
        )
        assertEquals(
            XmlResourceReference(null, "id", "foo_bar", false),
            "@id/foo.bar".parseXmlResourceReference()
        )
        assertEquals(
            XmlResourceReference(null, "id", "foo", true),
            "@+id/foo".parseXmlResourceReference()
        )
        assertEquals(
            XmlResourceReference("android", "id", "foo", false),
            "@android:id/foo".parseXmlResourceReference()
        )
        assertEquals(
            XmlResourceReference("android", "id", "foo", true),
            "@+android:id/foo".parseXmlResourceReference()
        )
        assertEquals(
            XmlResourceReference("android", "id", "foo", false),
            "@id/android:foo".parseXmlResourceReference()
        )
        assertEquals(
            XmlResourceReference("android", "id", "foo", true),
            "@+id/android:foo".parseXmlResourceReference()
        )
    }

    @Test fun xmlResourceReferenceParsingMustStartWithAt() {
        try {
            "id/foo".parseXmlResourceReference()
            fail()
        } catch (e: IllegalArgumentException) {
            assertEquals("Reference must start with '@': id/foo", e.message)
        }
    }

    @Test fun xmlResourceReferenceParsingMustContainType() {
        try {
            "@android:foo".parseXmlResourceReference()
            fail()
        } catch (e: IllegalArgumentException) {
            assertEquals("Invalid resource format: @android:foo", e.message)
        }
        try {
            "@foo".parseXmlResourceReference()
            fail()
        } catch (e: IllegalArgumentException) {
            assertEquals("Invalid resource format: @foo", e.message)
        }
    }

    @Test fun xmlResourceReferenceParsingNamespaceMustNotBeEmpty() {
        try {
            "@:id/foo".parseXmlResourceReference()
            fail()
        } catch (e: IllegalArgumentException) {
            assertEquals("Namespace cannot be empty: @:id/foo", e.message)
        }
    }

    @Test fun xmlResourceReferenceParsingNameMustNotBeEmpty() {
        try {
            "@android:id/".parseXmlResourceReference()
            fail()
        } catch (e: IllegalArgumentException) {
            assertEquals("Name cannot be empty: @android:id/", e.message)
        }
        try {
            "@id/android:".parseXmlResourceReference()
            fail()
        } catch (e: IllegalArgumentException) {
            assertEquals("Name cannot be empty: @id/android:", e.message)
        }
    }

    @Test fun xmlResourceReferenceParsingTypeMustNotBeEmpty() {
        try {
            "@/foo".parseXmlResourceReference()
            fail()
        } catch (e: IllegalArgumentException) {
            assertEquals("Type cannot be empty: @/foo", e.message)
        }
        try {
            "@android:/foo".parseXmlResourceReference()
            fail()
        } catch (e: IllegalArgumentException) {
            assertEquals("Type cannot be empty: @android:/foo", e.message)
        }
    }
}
