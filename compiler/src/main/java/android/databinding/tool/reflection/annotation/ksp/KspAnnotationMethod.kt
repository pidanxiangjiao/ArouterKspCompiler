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
import android.databinding.tool.ksp.isDeclaredType
import android.databinding.tool.ksp.isStatic
import android.databinding.tool.reflection.ModelClass
import android.databinding.tool.reflection.ModelMethod
import android.databinding.tool.reflection.SdkUtil
import android.databinding.tool.reflection.TypeUtil
import com.google.devtools.ksp.isProtected
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType

class KspAnnotationMethod(
    private val mDeclaringType: KSType,
    private val mExecutableElement: KSFunctionDeclaration
) : ModelMethod() {
    private var mApiLevel: Int = -1 // calculated on demand
    private var mReceiverType: ModelClass? = null

    init {

    }

    override fun getReceiverType(): ModelClass {
        if (mReceiverType == null) {
            mReceiverType = findReceiverType(mDeclaringType)
            if (mReceiverType == null) {
                mReceiverType = KspAnnotationClass(mDeclaringType)
            }
        }
        return mReceiverType as ModelClass
    }

    override fun getDeclaringClass(): ModelClass {
        return KspAnnotationClass(mDeclaringType)
    }

    private fun findReceiverType(subType: KSType): ModelClass? {
        val supers = (subType.declaration as KSClassDeclaration).superTypes
        for (superType in supers) {
            val declaredType = superType.resolve()
            if (declaredType.declaration.isDeclaredType()) {
                val inSuper = findReceiverType(declaredType)
                if (inSuper != null) {
                    return inSuper
                } else if (hasExecutableMethod(declaredType)) {
                    return KspAnnotationClass(declaredType)
                }
            }
        }
        return null
    }

    private fun hasExecutableMethod(declaredType: KSType): Boolean {
        val enclosing = mExecutableElement.parentDeclaration
        val typeElement = declaredType.declaration as KSClassDeclaration
        for (element in typeElement.getAllFunctions()) {
            if (element == mExecutableElement
                || resolver.overrides(mExecutableElement, element, enclosing as KSClassDeclaration)
            ) {
                return true
            }
        }
        return false
    }


    override fun getParameterTypes(): Array<ModelClass> {
        val parameters = mExecutableElement.parameters.map { it.type }
        return Array(parameters.size) { i ->
            KspAnnotationClass(parameters[i].resolve())
        }
    }

    override fun getName(): String {
        return mExecutableElement.simpleName.toString()
    }

    override fun getReturnType(args: List<ModelClass>): ModelClass {
        val returnType = mExecutableElement.returnType?.resolve()
        // TODO: support argument-supplied types
        // for example: public T[] toArray(T[] arr)
        returnType?.let {
            return KspAnnotationClass(it)
        } ?:run {
            throw RuntimeException("getReturnType error ! $mExecutableElement")
        }

    }

    override fun isVoid(): Boolean {
        return mExecutableElement.returnType?.resolve() == resolver.builtIns.unitType
    }

    override fun isPublic(): Boolean {
        return mExecutableElement.isPublic()
    }

    override fun isProtected(): Boolean {
        return mExecutableElement.isProtected()
    }

    override fun isStatic(): Boolean {
        return mExecutableElement.isStatic()
    }

    override fun isAbstract(): Boolean {
        return mExecutableElement.isAbstract
    }

    override fun getBindableAnnotation(): BindableCompat? {
        return extractFrom(mExecutableElement)
    }

    override fun getMinApi(): Int {
        if (mApiLevel == -1) {
            mApiLevel = SdkUtil.get().getMinApi(this)
        }
        return mApiLevel
    }

    override fun getJniDescription(): String {
        return TypeUtil.getInstance().getDescription(this)
    }

    override fun isVarArgs(): Boolean {
        return mExecutableElement.parameters.any { it.isVararg }
    }

    override fun toString(): String {
        return "AnnotationMethod{" +
//                " mMethod=" + mMethod +
                ", mDeclaringType=" + mDeclaringType +
                ", mExecutableElement=" + mExecutableElement +
                ", mApiLevel=" + mApiLevel +
                '}'
    }

    companion object {
        private val resolver: Resolver
            get() = KspAnnotationAnalyzer.get().kspResolver
//
//        private val elementUtils: Elements
//            get() = AnnotationAnalyzer.get().mProcessingEnv.elementUtils
    }
}
