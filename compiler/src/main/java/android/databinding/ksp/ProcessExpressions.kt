package android.databinding.ksp

import android.databinding.annotationprocessor.ProcessingStep
import android.databinding.tool.CompilerArguments
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment

class ProcessExpressions: ProcessingStep() {
    //TODO ksp
    override fun onHandleKspStep(
        roundEnvironment: Resolver?,
        processingEnvironment: SymbolProcessorEnvironment?,
        args: CompilerArguments?
    ): Boolean {

        return true

    }

    override fun onHandleStep(
        roundEnvironment: RoundEnvironment?,
        processingEnvironment: ProcessingEnvironment?,
        args: CompilerArguments?
    ): Boolean {
        return true
    }

    override fun onProcessingOver(
        roundEnvironment: RoundEnvironment?,
        processingEnvironment: ProcessingEnvironment?,
        args: CompilerArguments?
    ) {

    }


}