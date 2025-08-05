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

package android.databinding.ksp


import android.databinding.annotationprocessor.BindableBag
import android.databinding.annotationprocessor.Callback
import android.databinding.annotationprocessor.ProcessBindable
import android.databinding.annotationprocessor.ProcessingStep
import android.databinding.tool.CompilerArguments
import android.databinding.tool.CompilerChef
import android.databinding.tool.Context
import android.databinding.tool.processing.Scope
import android.databinding.tool.processing.ScopedException
import android.databinding.tool.store.GenClassInfoLog
import android.databinding.tool.util.Preconditions
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated


class DataBindingSymbolProcessor(
    private val environment: SymbolProcessorEnvironment,
    private val logger: KSPLoggerWrapper,
) : SymbolProcessor {

    override fun finish() {
        super.finish()
    }

    private var mProcessingSteps: List<ProcessingStep>? = null
    private var mCompilerArgs: CompilerArguments? = null

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (mProcessingSteps == null) {
            readArguments()
            initProcessingSteps()
        }
        if (mCompilerArgs == null) {
            return emptyList()
        }
        mCompilerArgs?.let {
            if (it.isTestVariant && !it.isEnabledForTests &&
                !it.isLibrary
            ) {
                logger.warn("data binding processor is invoked but not enabled, skipping...")
                return emptyList()
            }
            Context.initForKsp(resolver, environment, mCompilerArgs!!)

            mProcessingSteps?.forEach { step ->
                try {
                    step.runKspStep(resolver, environment, it)
                } catch (e: Exception) {
                    e.printStackTrace()
                    logger.error("Exception while handling step $step")
                }
            }

//            if (resolver.getAllFiles().none { it is KSAnnotated && it.validate() }) {
//                processingSteps?.forEach { step ->
//                    step.onProcessingOver(resolver, environment, mCompilerArgs!!)
//                }
//                Scope.assertNoError()
//            }
        }
        Scope.assertNoError()
        return emptyList()
    }

    private fun initProcessingSteps() {
        val processBindable = ProcessBindable()
        mProcessingSteps = listOf(
            ProcessMethodAdapters(),
            //TODO ksp
//            ProcessExpressions(),
//            processBindable,
        )
        val dataBinderWriterCallback = object : Callback {
            var chef: CompilerChef? = null
            var modulePackages: List<String>? = null
            var brVariableLookup: BindableBag.BRMapping? = null
            var writtenMapper = false

            override fun onChefReady(
                chef: CompilerChef?,
                classInfoLog: GenClassInfoLog?
            ) {
                Preconditions.checkNull(this.chef, "Cannot set compiler chef twice")
                chef?.addBRVariables(processBindable)
                this.chef = chef
                considerWritingMapper()
            }

            private fun considerWritingMapper() {
                if (writtenMapper || chef == null || brVariableLookup == null) {
                    return
                }
                val justLibrary = mCompilerArgs?.isLibrary == true &&
                        mCompilerArgs?.isTestVariant == false
                if (justLibrary && mCompilerArgs?.isEnableV2 == false) {
                    return
                }
                writtenMapper = true
                //TODO ksp
//                chef?.writeDataBinderMapper(processingEnv, mCompilerArgs, brVariableLookup, modulePackages)
            }

            override fun onBrWriterReady(
                brWithValues: BindableBag.BRMapping?,
                brPackages: List<String>?
            ) {
                Preconditions.checkNull(brVariableLookup, "Cannot set br writer twice")
                brVariableLookup = brWithValues
                modulePackages = brPackages
                considerWritingMapper()
            }
        }
        //TODO ksp
//        val javaFileWriter = AnnotationJavaFileWriter(processingEnv)
        mProcessingSteps?.forEach { step ->
//            step.mJavaFileWriter = javaFileWriter //TODO ksp
            step.mCallback = dataBinderWriterCallback
        }
    }

    private fun readArguments() {
        if (mCompilerArgs != null) return
        try {
            val options = environment.options
            mCompilerArgs = CompilerArguments.readFromOptions(options)
            logger.logging("processor args: $mCompilerArgs")
            ScopedException.encodeOutput(mCompilerArgs?.printEncodedErrorLogs?:false)
        } catch (t: Throwable) {
            t.printStackTrace()
            val allParam: String =
                environment.options.entries.stream()
                    .map<String> { entry: Map.Entry<String, String> -> entry.key + " : " + entry.value }
                    .collect(java.util.stream.Collectors.joining("\n"))
            throw java.lang.RuntimeException(
                "Failed to parse data binding compiler options. Params:\n"
                        + allParam, t
            )
        }
    }

}
