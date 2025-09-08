# ArouterKspCompiler
Arouter KSP annotation processor


dev/db

https://slack-chats.kotlinlang.org/t/9579634/it-seems-that-there-is-not-going-to-be-support-for-https-iss


Issue List

1.A problem was found with the configuration of task '::bundleLibCompileToJarDebug' (type 'BundleLibraryClassesJar').
- In plugin 'com.android.internal.version-check' type 'com.android.build.gradle.internal.tasks.BundleLibraryClassesJar' property 'dataBindingExcludeDelegate.exportClassListLocation' specifies file '/data_binding_export_class_list/Debug/compileDebugJavaWithJavac/out.jar' which doesn't exist.

  Reason: An input file was expected to be present but it doesn't exist.

  Possible solutions:
    1. Make sure the file exists before the task is called.
    2. Make sure that the task which produces the file is declared as an input.

  For more information, please refer to https://docs.gradle.org/8.9/userguide/validation_problems.html#input_file_does_not_exist in the Gradle documentation.

Task :bizlib:compileDebugJavaWithJavac output->/bizlib/build/intermediates/data_binding_export_class_list/debug/compileDebugJavaWithJavac/out.jar

FileUtils.write(compilerArgs.getExportClassListOutFile(), out)  


2.An operation is not implemented: Not yet implemented
getAllFields