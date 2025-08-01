/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.databinding.kspprocessor


import android.databinding.annotationprocessor.ProcessBindable
import android.databinding.annotationprocessor.ProcessDataBinding
import android.databinding.annotationprocessor.ProcessExpressions
import android.databinding.annotationprocessor.ProcessMethodAdapters
import android.databinding.tool.CompilerArguments
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import javax.security.auth.callback.Callback


class DataBindingSymbolProcessor(val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

    override fun finish() {
        super.finish()
    }

    private var processingSteps: List<ProcessDataBinding.ProcessingStep>? = null
    private var compilerArgs: CompilerArguments? = null

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (processingSteps == null) {
            readArguments()
            initProcessingSteps()
        }
        if (compilerArgs == null) {
            return emptyList()
        }

//        var done = true
//        Context.init(environment, compilerArgs!!)
//        for (step in processingSteps!!) {
//            try {
//                done = step.runStep(resolver, environment, compilerArgs!!) && done
//            } catch (e: Exception) {
//                // 处理异常
//            }
//        }

        return emptyList()
    }

    private fun initProcessingSteps() {
        val processBindable = ProcessBindable()
        processingSteps = listOf(
            ProcessMethodAdapters(),
            ProcessExpressions(),
            processBindable,
        )
        val dataBinderWriterCallback = object : Callback {
        }
//        val fileWriter = KspJavaFileWriter(environment.codeGenerator)
//        processingSteps!!.forEach {
//            it.javaFileWriter = fileWriter
//            it.callback = dataBinderWriterCallback
//        }
    }

    private fun readArguments() {
        if (compilerArgs != null) return
        val options = environment.options
        compilerArgs = CompilerArguments.readFromOptions(options)
    }


}
