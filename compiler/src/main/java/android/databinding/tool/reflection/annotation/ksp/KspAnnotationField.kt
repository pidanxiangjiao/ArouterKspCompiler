/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.databinding.tool.reflection.annotation.ksp

import android.databinding.tool.BindableCompat
import android.databinding.tool.BindableCompat.Companion.extractFrom
import android.databinding.tool.ksp.isInterface
import android.databinding.tool.ksp.isStatic
import android.databinding.tool.reflection.ModelClass
import android.databinding.tool.reflection.ModelField
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier

class KspAnnotationField(val mDeclaredClass: KSType, val mField: KSPropertyDeclaration) :
    ModelField() {

    val fieldKspType by lazy(LazyThreadSafetyMode.NONE) {
        mField.type.resolve()
    }


    override fun toString(): String {
        return mField.toString()
    }

    override fun getName(): String {
        return mField.simpleName.toString()
    }

    override fun isPublic(): Boolean {
        return mField.isPublic()
    }

    override fun isStatic(): Boolean {
        return mField.isStatic()
    }

    override fun isFinal(): Boolean {
        if (Modifier.FINAL in mField.modifiers) return true
        return !mField.isMutable
    }

    override fun getFieldType(): ModelClass {
        return KspAnnotationClass(fieldKspType)
    }

    override fun getBindableAnnotation(): BindableCompat? {
        return extractFrom(mField)
    }

    override fun hashCode(): Int {
        return mField.simpleName.hashCode()
    }

    override fun equals(obj: Any?): Boolean {
        if (obj is KspAnnotationField) {
            val that = obj
            return mDeclaredClass.equals(that.mDeclaredClass)
                    && fieldKspType.equals(that.fieldKspType)
                    && mField.simpleName == that.mField.simpleName
        } else {
            return false
        }
    }
}
