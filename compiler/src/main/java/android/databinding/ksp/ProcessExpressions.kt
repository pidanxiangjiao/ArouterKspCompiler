package android.databinding.ksp

import android.databinding.annotationprocessor.ProcessExpressions
import android.databinding.annotationprocessor.ProcessExpressionsFromV1Compat
import android.databinding.annotationprocessor.ProcessingStep
import android.databinding.tool.CompilerArguments
import android.databinding.tool.CompilerChef
import android.databinding.tool.processing.Scope
import android.databinding.tool.processing.ScopedException
import android.databinding.tool.reflection.ModelAnalyzer
import android.databinding.tool.store.GenClassInfoLog
import android.databinding.tool.store.ResourceBundle
import android.databinding.tool.util.GenerationalClassUtil
import android.databinding.tool.util.L
import android.databinding.tool.util.LoggedErrorException
import android.databinding.tool.util.StringUtils
import android.databinding.tool.writer.BindingMapperWriter
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import org.apache.commons.io.Charsets
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment

class ProcessExpressions: ProcessingStep() {
    //TODO ksp
    override fun onHandleKspStep(
        resolver: Resolver,
        processingEnvironment: SymbolProcessorEnvironment,
        args: CompilerArguments
    ): Boolean {
        try {
            val resourceBundle = ResourceBundle(
                args.modulePackage,
                ModelAnalyzer.getInstance().libTypes.useAndroidX
            )
            L.d("creating resource bundle for %s", args.modulePackage)

            val intermediateList: MutableList<ProcessExpressions.IntermediateV2>
            var infoLog: GenClassInfoLog? = null
            var v1CompatChef: CompilerChef? = null

            if (args.isEnableV2) {
                try {
                    L.d("trying to read class log from %s", args.classLogDir)
                    infoLog = ResourceBundle.loadClassInfoFromFolder(args.classLogDir)
                    L.d("done reading class log. cools.")
                } catch (e: IOException) {
                    L.d(e, "failed to read class log :/")
                    infoLog = GenClassInfoLog()
                    Scope.defer(
                        ScopedException(
                            "cannot load the info log from %s", args.classLogDir
                        )
                    )
                }
                resourceBundle.addDependencyLayouts(infoLog!!)
                intermediateList = mutableListOf()
                v1CompatChef = ProcessExpressionsFromV1Compat(
                    null,
                    resolver,
                    args,
                    loadDependencyIntermediates(),
                    writer
                ).generate()
            } else {
                intermediateList = loadDependencyIntermediates().toMutableList()
                for (intermediate in intermediateList) {
                    try {
                        try {
                            intermediate.appendTo(resourceBundle, false)
                        } catch (throwable: Throwable) {
                            L.e(throwable, "unable to prepare resource bundle")
                        }
                    } catch (e: LoggedErrorException) {
                        // This will be logged later
                    }
                }
            }

            val mine = createIntermediateFromLayouts(args.layoutInfoDir, intermediateList)
            if (!args.isEnableV2) {
                mine.updateOverridden(resourceBundle)
                intermediateList.add(mine)
                saveIntermediate(args, mine)
            }
            mine.appendTo(resourceBundle, true)

            // generate resource bundle
            try {
                writeResourceBundle(resourceBundle, args, infoLog, v1CompatChef)
            } catch (t: Throwable) {
                L.e(t, "cannot generate view binders")
            }
        } catch (e: LoggedErrorException) {
            // This will be logged later
        }
        return true

    }

    private fun writeResourceBundle(
        resourceBundle: ResourceBundle,
        compilerArgs: CompilerArguments,
        classInfoLog: GenClassInfoLog?,
        v1CompatChef: CompilerChef?
    ) {
        val compilerChef = CompilerChef.createChef(resourceBundle, writer, compilerArgs)
        compilerChef.setV1CompatChef(v1CompatChef)
        compilerChef.sealModels()
        if (compilerArgs.isLibrary
            || (!compilerArgs.isTestVariant && !compilerArgs.isFeature)
        ) {
            compilerChef.writeComponent()
        }
        if (compilerChef.hasAnythingToGenerate()) {
            if (!compilerArgs.isEnableV2) {
                compilerChef.writeViewBinderInterfaces(
                    compilerArgs.isLibrary && !compilerArgs.isTestVariant
                )
            }
            if (compilerArgs.isApp != compilerArgs.isTestVariant
                || (compilerArgs.isEnabledForTests && !compilerArgs.isLibrary)
                || compilerArgs.isEnableV2
            ) {
                compilerChef.writeViewBinders(compilerArgs.minApi)
            }
        }
        if (compilerArgs.isLibrary
            && !compilerArgs.isTestVariant
            && compilerArgs.exportClassListOutFile == null
        ) {
            L.e("When compiling a library module, build info must include exportClassListTo path")
        }
        if (compilerArgs.isLibrary && !compilerArgs.isTestVariant) {
            val classNames = compilerChef.getClassesToBeStripped().toMutableSet()
            if (v1CompatChef != null) {
                classNames.addAll(v1CompatChef.getClassesToBeStripped())
                classNames.add(BindingMapperWriter.v1CompatMapperPkg(compilerChef.useAndroidX()))
            }
            val out = classNames.joinToString(StringUtils.LINE_SEPARATOR)
            L.d(
                "Writing list of classes to %s . \nList:%s",
                compilerArgs.exportClassListOutFile, out
            )
            try {
                FileUtils.write(compilerArgs.exportClassListOutFile, out)
            } catch (e: IOException) {
                L.e(e, "Cannot create list of written classes")
            }
        }
        mCallback.onChefReady(compilerChef, classInfoLog)
    }


    private fun saveIntermediate(
        args: CompilerArguments,
        intermediate: ProcessExpressions.IntermediateV2
    ) {
        GenerationalClassUtil.get().write(
            args.modulePackage,
            GenerationalClassUtil.ExtensionFilter.LAYOUT, intermediate
        )
    }

    private fun createIntermediateFromLayouts(
        layoutInfoDir: File,
        intermediateList: List<ProcessExpressions.IntermediateV2>
    ): ProcessExpressions.IntermediateV2 {
        L.d("creating intermediate list from input layouts of %s", layoutInfoDir)
        val excludeList = mutableSetOf<String>()
        for (lib in intermediateList) {
            excludeList.addAll(lib.mLayoutInfoMap.keys)
        }
        val result = ProcessExpressions.IntermediateV2()
        if (!layoutInfoDir.isDirectory) {
            // it is a zip in blaze / bazel.
            L.d("trying to load layout info from zip file")
            if (layoutInfoDir.exists()) {
                L.d("found zip file %s", layoutInfoDir)
                try {
                    loadLayoutInfoFromZipFile(layoutInfoDir, result, excludeList)
                    L.d("done loading from zip file")
                } catch (e: IOException) {
                    L.e(e, "error while trying to load layout info from %s", layoutInfoDir)
                }
            } else {
                L.d("layout info folder does not exist, skipping for %s", layoutInfoDir.path)
            }
        } else {
            // it is a directory, search sub folders.
            FileUtils.listFiles(layoutInfoDir, arrayOf("xml"), true).forEach { layoutFile ->
                if (excludeList.contains(layoutFile.name)) return@forEach
                L.d("found xml file %s", layoutFile.absolutePath)
                try {
                    result.addEntry(layoutFile.name, FileUtils.readFileToString(layoutFile))
                } catch (e: IOException) {
                    L.e(e, "cannot load layout file information. Try a clean build")
                }
            }
            // also accept zip files
            FileUtils.listFiles(layoutInfoDir, arrayOf("zip"), true).forEach { zipFile ->
                try {
                    L.d("found zip file %s", zipFile.absolutePath)
                    loadLayoutInfoFromZipFile(zipFile, result, excludeList)
                } catch (e: IOException) {
                    L.e(e, "error while reading layout zip file %s", zipFile)
                }
            }
        }
        L.d("done loading info files")
        return result
    }
    @Throws(IOException::class)
    private fun loadLayoutInfoFromZipFile(
        zipFile: File,
        result: ProcessExpressions.IntermediateV2,
        excludeList: Set<String>
    ) {
        ZipFile(zipFile).use { zf ->
            L.d("checking zip file %s", zipFile)
            val entries = zf.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                L.d("checking entry %s", entry.name)
                if (excludeList.contains(entry.name)) {
                    L.d("exclude entry %s", entry.name)
                    continue
                }
                L.d("use entry %s", entry.name)
                try {
                    zf.getInputStream(entry).use { input ->
                        val content = IOUtils.toString(input, Charsets.UTF_8)
                        result.addEntry(entry.name, content)
                        L.d("loaded entry %s", entry.name)
                    }
                } catch (e: IOException) {
                    L.e(e, "cannot load layout file information. Try a clean build")
                }
            }
            L.d("done loading zip file %s", zipFile)
        }
    }

    private fun loadDependencyIntermediates(): List<ProcessExpressions.IntermediateV2> {
        val original: List<ProcessExpressions.Intermediate> =
            GenerationalClassUtil.get().load(
                GenerationalClassUtil.ExtensionFilter.LAYOUT, ProcessExpressions.Intermediate::class.java
        )
        return original.map { intermediate ->
            val updated = intermediate.upgrade()
            require(updated is ProcessExpressions.IntermediateV2) {
                "Incompatible data binding dependency. Please update your dependencies or recompile them with application module's data binding version."
            }
            updated
        }
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