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
./gradlew :publishLocal


3.ERROR: Could not find accessor viewmodel.xxx
bindingTarget.resolveMultiSetters()
FieldAccessExpr.resolveType()
mGetter = resolvedType.findGetterOrField(mName, isStatic)

java.lang.NoClassDefFoundError: android/databinding/Bindable

java.util.NoSuchElementException: No TypeParameter found for index T
  at com.squareup.kotlinpoet.ksp.TypeParameterResolver$Companion$EMPTY$1.get(TypeParameterResolver.kt:45)
  at com.squareup.kotlinpoet.ksp.KsTypesKt.toTypeName(KsTypes.kt:76)
  at com.squareup.kotlinpoet.ksp.KsTypesKt.toTypeName(KsTypes.kt:65)
  at com.squareup.kotlinpoet.ksp.KsTypesKt.toTypeName$default(KsTypes.kt:63)
  at android.databinding.tool.reflection.annotation.ksp.KspAnnotationClass.equals(KspAnnotationClass.kt:159)
  at android.databinding.tool.reflection.annotation.ksp.KspAnnotationClass.isAssignableFrom(KspAnnotationClass.kt:172)


java.lang.IllegalArgumentException: couldn't make a guess for
  at com.squareup.javapoet.Util.checkArgument(Util.java:64)
  at com.squareup.javapoet.ClassName.bestGuess(ClassName.java:183)
  at android.databinding.tool.ext.ExtKt.toTypeName(ext.kt:260)
  at android.databinding.tool.ext.ExtKt.toTypeName(ext.kt:201)
  at android.databinding.tool.reflection.ModelClass.getTypeName(ModelClass.kt:335)
  at android.databinding.tool.reflection.ModelClass$isObject$2.invoke(ModelClass.kt:146)
  at android.databinding.tool.reflection.ModelClass$isObject$2.invoke(ModelClass.kt:145)
  at kotlin.UnsafeLazyImpl.getValue(Lazy.kt:81)
  at android.databinding.tool.reflection.ModelClass.isObject(ModelClass.kt:145)
  at android.databinding.tool.store.SetterStore.calculateConversionPriority(SetterStore.java:1064)
  at android.databinding.tool.store.SetterStore.isBetterParameter(SetterStore.java:943)
  at android.databinding.tool.store.SetterStore.getBestSetter(SetterStore.java:803)
  at android.databinding.tool.store.SetterStore.getSetterCall(SetterStore.java:625)
  at android.databinding.tool.Binding.resolveSetterCall(Binding.java:140)
  at android.databinding.tool.Binding.getSetterCall(Binding.java:108)
  at android.databinding.tool.Binding.injectSafeUnboxing(Binding.java:245)
  at android.databinding.tool.BindingTarget.injectSafeUnboxing(BindingTarget.java:160)
  at android.databinding.tool.LayoutBinder.<init>(LayoutBinder.java:156)
  at android.databinding.tool.DataBinder.<init>(DataBinder.java:61)


