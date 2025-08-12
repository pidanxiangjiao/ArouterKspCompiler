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
import android.databinding.tool.reflection.ModelClass
import android.databinding.tool.reflection.ModelField
import android.databinding.tool.reflection.annotation.AnnotationAnalyzer
import android.databinding.tool.reflection.annotation.AnnotationClass
import javax.lang.model.element.Modifier
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType

internal class KspAnnotationField(val mDeclaredClass: DeclaredType, val mField: VariableElement) :
    ModelField() {
    override fun toString(): String {
        return mField.toString()
    }

    override fun getName(): String {
        return mField.simpleName.toString()
    }

    override fun isPublic(): Boolean {
        return mField.modifiers.contains(Modifier.PUBLIC)
    }

    override fun isStatic(): Boolean {
        return mField.modifiers.contains(Modifier.STATIC)
    }

    override fun isFinal(): Boolean {
        return mField.modifiers.contains(Modifier.FINAL)
    }

    override fun getFieldType(): ModelClass {
        val typeUtils = AnnotationAnalyzer.get().typeUtils
        val type = typeUtils.asMemberOf(mDeclaredClass, mField)
        return AnnotationClass(type)
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
            val typeUtils = AnnotationAnalyzer.get().typeUtils
            return typeUtils.isSameType(mDeclaredClass, that.mDeclaredClass)
                    && typeUtils.isSameType(mField.asType(), that.mField.asType())
                    && mField.simpleName == that.mField.simpleName
        } else {
            return false
        }
    }
}
