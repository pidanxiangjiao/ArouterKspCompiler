package android.databinding.ksp


import android.databinding.tool.ksp.KspLogger
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider


class DataBindingSymbolProcessorProvider: SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        KspLogger.logger = environment.logger
        return DataBindingSymbolProcessor(
            environment,
        )
    }
}
