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
package android.databinding.ksp

import android.databinding.annotationprocessor.ProcessingStep
import android.databinding.ksp.AnnotationUtil.getElementsAnnotatedWith
import android.databinding.tool.BindingAdapterCompat.Companion.create
import android.databinding.tool.CompilerArguments
import android.databinding.tool.ksp.KspLogger
import android.databinding.tool.reflection.ModelAnalyzer.Companion.getInstance
import android.databinding.tool.store.SetterStore
import android.databinding.tool.util.LoggedErrorException
import android.databinding.tool.util.Preconditions
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import java.io.IOException
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment

class ProcessMethodAdapters : ProcessingStep() {

    companion object {
        private const val INVERSE_BINDING_EVENT_ATTR_SUFFIX = "AttrChanged"
    }

    override fun onHandleKspStep(
        resolver: Resolver,
        processingEnvironment: SymbolProcessorEnvironment,
        args: CompilerArguments
    ): Boolean {
        KspLogger.logging("processing adapters")
        val modelAnalyzer = getInstance()
        Preconditions.checkNotNull(
            modelAnalyzer, "Model analyzer should be"
                    + " initialized first"
        )
        val store = SetterStore.get()

        clearIncrementalClasses(resolver, store)

        addBindingAdapters(resolver, processingEnvironment, store)

        //TODO ksp
//        addRenamed(roundEnv, store);
//        addConversions(roundEnv, store);
//        addUntaggable(roundEnv, store);
//        addInverseAdapters(roundEnv, processingEnvironment, store);
//        addInverseBindingMethods(roundEnv, store);
//        addInverseMethods(roundEnv, processingEnvironment, store);
        try {
            try {
                store.write(args.modulePackage)
            } catch (e: IOException) {
                KspLogger.logging("Could not write BindingAdapter intermediate file.")
                KspLogger.exception(e)
            }
        } catch (e: LoggedErrorException) {
            // This will be logged later
        }
        return true
    }

    override fun onHandleStep(
        roundEnv: RoundEnvironment,
        processingEnvironment: ProcessingEnvironment,
        args: CompilerArguments
    ): Boolean {
        return true
    }

    override fun onProcessingOver(
        roundEnvironment: RoundEnvironment,
        processingEnvironment: ProcessingEnvironment,
        args: CompilerArguments
    ) {
    }

    private fun addBindingAdapters(
        resolver: Resolver,
        processingEnv: SymbolProcessorEnvironment,
        store: SetterStore
    ) {
        val libTypes = getInstance().libTypes
        for (element in getElementsAnnotatedWith(resolver, libTypes.bindingAdapterClass.name)) {
            try {
                if ((element !is KSFunctionDeclaration) || !element.isPublic()) {
                    KspLogger.logging("@BindingAdapter on invalid element: %s", element)
                    continue
                }
                val bindingAdapter = create(element)

                val function = element as? KSFunctionDeclaration ?: continue
                val parameters = function.parameters

                if (bindingAdapter.attributes.isEmpty()) {
                    KspLogger.logging("@BindingAdapter requires at least one attribute. $element")
                    continue
                }

                val takesComponent = takesComponent(function, resolver)
                val startIndex = 1 + (if (takesComponent) 1 else 0)
                val numAttributes = bindingAdapter.attributes.size
                val numAdditionalArgs = parameters.size - startIndex
                if (numAdditionalArgs == 2 * numAttributes) {
                    // This BindingAdapter takes old and new values. Make sure they are properly ordered
                    var hasParameterError = false
                    for (i in startIndex until (numAttributes + startIndex)) {
                        val type1 = parameters[i].type.resolve()
                        val type2 = parameters[i + numAttributes].type.resolve()
                        if (type1 != type2) {
                            KspLogger.error(
                                "BindingAdapter $function: old values should be followed " +
                                        "by new values. Parameter ${i + 1} must be the same type as parameter ${i + numAttributes + 1}.",
                                function
                            )
                            hasParameterError = true
                            break
                        }
                    }
                    if (hasParameterError) {
                        continue
                    }
                } else if (numAdditionalArgs != numAttributes) {
                    KspLogger.error(
                        "@BindingAdapter $function has $numAttributes attributes and $numAdditionalArgs value parameters. There should be $numAttributes or ${numAttributes * 2} value parameters.",
                        function
                    )
                    continue
                }

                warnAttributeNamespaces(element, bindingAdapter.attributes)

                try {
                    val attributes = bindingAdapter.attributes
                    if (numAttributes == 1) {
                        store.addBindingAdapter(
                            resolver,
                            attributes.first(),
                            function,
                            takesComponent
                        )
                    } else {
                        store.addBindingAdapter(
                            resolver,
                            attributes,
                            function,
                            takesComponent,
                            bindingAdapter.requireAll
                        )
                    }
                } catch (e: IllegalArgumentException) {
                    KspLogger.error("@BindingAdapter for duplicate View and parameter type: $element")
                }

            } catch (e: LoggedErrorException) {
                // This will be logged later
            }
        }
    }

    private fun takesComponent(
        executableElement: KSFunctionDeclaration,
        resolver: Resolver
    ): Boolean {
        val parameters = executableElement.parameters
        val viewElement = resolver.getClassDeclarationByName(resolver.getKSNameFromString("android.view.View"))?.asStarProjectedType()
        if (parameters.size < 2) {
            return false // Validation will fail in the caller
        }
        val parameter1 = parameters[0].type.resolve()
        viewElement?.let {
            if (it.isAssignableFrom(parameter1)) {
                return false // first parameter is a View
            }
        }
        val analyzer = getInstance()
        if (parameters.size < 3) {
            val viewStubProxy = resolver.getClassDeclarationByName(resolver.getKSNameFromString(analyzer.libTypes.viewStubProxy))?.asStarProjectedType()
            viewStubProxy?.let {
                if (!it.isAssignableFrom(parameter1)) {
                    KspLogger.error(
                        "@BindingAdapter $executableElement is applied to a method that has" +
                                " two parameters, the first must be a View type:", executableElement
                    )
                }
            }
            return false
        }
        val parameter2 = parameters[1].type.resolve()
        viewElement?.let {
            if (it.isAssignableFrom(parameter2)) {
                return true // second parameter is a View
            }
        }

        KspLogger.error(
            "@BindingAdapter %s is applied to a method that doesn't take " +
                    "a View subclass as the first or second parameter. When a BindingAdapter" +
                    " uses a DataBindingComponent, the component parameter is first and the " +
                    "View parameter is second, otherwise the View parameter is first.",
            executableElement
        )
        return false
    }

    private fun warnAttributeNamespace(element: KSFunctionDeclaration, attribute: String) {
        if (attribute.contains(":") && !attribute.startsWith("android:")) {
            KspLogger.warn("Application namespace for attribute $attribute will be ignored.")
        }
    }

    private fun warnAttributeNamespaces(element: KSFunctionDeclaration, attributes: Array<String>) {
        attributes.forEach { attribute ->
            warnAttributeNamespace(element, attribute)
        }
    }


    //    private void addRenamed(RoundEnvironment roundEnv, SetterStore store) {
    //        LibTypes libTypes = ModelAnalyzer.getInstance().libTypes;
    //        Class<? extends Annotation> bindingMethodsClass = libTypes.getBindingMethodsClass();
    //        for (Element element : AnnotationUtil
    //                .getElementsAnnotatedWith(roundEnv, bindingMethodsClass)) {
    //            BindingMethodsCompat bindingMethods = BindingMethodsCompat.create(element);
    //
    //            for (BindingMethodsCompat.BindingMethodCompat bindingMethod :
    //                    bindingMethods.getMethods()) {
    //                try {
    //                    final String attribute = bindingMethod.getAttribute();
    //                    final String method = bindingMethod.getMethod();
    //                    warnAttributeNamespace(element, attribute);
    //                    String type = bindingMethod.getType();
    //                    store.addRenamedMethod(attribute, type, method, (TypeElement) element);
    //                } catch (LoggedErrorException e) {
    //                    // this will be logged later
    //                }
    //            }
    //        }
    //    }
    //    private void addConversions(RoundEnvironment roundEnv, SetterStore store) {
    //        Class<? extends Annotation> bindingConversionClass = ModelAnalyzer.getInstance()
    //                .libTypes.getBindingConversionClass();
    //        for (Element element : AnnotationUtil
    //                .getElementsAnnotatedWith(roundEnv, bindingConversionClass)) {
    //            try {
    //                if (element.getKind() != ElementKind.METHOD ||
    //                        !element.getModifiers().contains(Modifier.STATIC) ||
    //                        !element.getModifiers().contains(Modifier.PUBLIC)) {
    //                    L.e(element, "@BindingConversion is only allowed on public static " +
    //                            "methods %s", element);
    //                    continue;
    //                }
    //
    //                ExecutableElement executableElement = (ExecutableElement) element;
    //                if (executableElement.getParameters().size() != 1) {
    //                    L.e(element, "@BindingConversion method should have one parameter %s",
    //                            element);
    //                    continue;
    //                }
    //                if (executableElement.getReturnType().getKind() == TypeKind.VOID) {
    //                    L.e(element, "@BindingConversion method must return a value %s", element);
    //                    continue;
    //                }
    //                store.addConversionMethod(executableElement);
    //            } catch (LoggedErrorException e) {
    //                // this will be logged later
    //            }
    //        }
    //    }
    //    private void addInverseAdapters(RoundEnvironment roundEnv,
    //                                    ProcessingEnvironment processingEnv, SetterStore store) {
    //        LibTypes libTypes = ModelAnalyzer.getInstance().libTypes;
    //        Class<? extends Annotation> inverseBindingAdapterClass =
    //                libTypes.getInverseBindingAdapterClass();
    //        for (Element element : AnnotationUtil
    //                .getElementsAnnotatedWith(roundEnv, inverseBindingAdapterClass)) {
    //            try {
    //                if (!element.getModifiers().contains(Modifier.PUBLIC)) {
    //                    L.e(element,
    //                            "@InverseBindingAdapter must be associated with a public method");
    //                    continue;
    //                }
    //                ExecutableElement executableElement = (ExecutableElement) element;
    //                if (executableElement.getReturnType().getKind() == TypeKind.VOID) {
    //                    L.e(element, "@InverseBindingAdapter must have a non-void return type");
    //                    continue;
    //                }
    //                final InverseBindingAdapterCompat inverseBindingAdapter =
    //                        InverseBindingAdapterCompat.create(executableElement);
    //                final String attribute = inverseBindingAdapter.getAttribute();
    //                warnAttributeNamespace(element, attribute);
    //                final String event = inverseBindingAdapter.getEvent().isEmpty()
    //                        ? inverseBindingAdapter.getAttribute() + INVERSE_BINDING_EVENT_ATTR_SUFFIX
    //                        : inverseBindingAdapter.getEvent();
    //                warnAttributeNamespace(element, event);
    //                final boolean takesComponent = takesComponent(executableElement, processingEnv);
    //                final int expectedArgs = takesComponent ? 2 : 1;
    //                final int numParameters = executableElement.getParameters().size();
    //                if (numParameters != expectedArgs) {
    //                    L.e(element, "@InverseBindingAdapter %s takes %s parameters, but %s "
    //                            + "parameters were expected", element, numParameters, expectedArgs);
    //                    continue;
    //                }
    //                try {
    //                    store.addInverseAdapter(processingEnv, attribute, event, executableElement,
    //                            takesComponent);
    //                } catch (IllegalArgumentException e) {
    //                    L.e(element, "@InverseBindingAdapter for duplicate View and parameter "
    //                            + "type: %s", element);
    //                }
    //            } catch (LoggedErrorException e) {
    //                // This will be logged later
    //            }
    //        }
    //    }
    //    private void addInverseBindingMethods(RoundEnvironment roundEnv, SetterStore store) {
    //        LibTypes libTypes = ModelAnalyzer.getInstance().libTypes;
    //        Class<? extends Annotation> inverseBindingMethodsClass = libTypes
    //                .getInverseBindingMethodsClass();
    //        for (Element element : AnnotationUtil
    //                .getElementsAnnotatedWith(roundEnv, inverseBindingMethodsClass)) {
    //            InverseBindingMethodsCompat bindingMethods =
    //                    InverseBindingMethodsCompat.create(element);
    //
    //            for (InverseBindingMethodsCompat.InverseBindingMethodCompat bindingMethod :
    //                    bindingMethods.getMethods()) {
    //                try {
    //                    final String attribute = bindingMethod.getAttribute();
    //                    final String method = bindingMethod.getMethod();
    //                    final String event = bindingMethod.getEvent().isEmpty()
    //                            ? bindingMethod.getAttribute() + INVERSE_BINDING_EVENT_ATTR_SUFFIX
    //                            : bindingMethod.getEvent();
    //                    warnAttributeNamespace(element, attribute);
    //                    warnAttributeNamespace(element, event);
    //                    String type = bindingMethod.getType();
    //                    store.addInverseBindingMethod(attribute, event, type, method,
    //                            (TypeElement) element);
    //                } catch (LoggedErrorException e) {
    //                    // This will be logged later
    //                }
    //            }
    //        }
    //    }
    //    private void addInverseMethods(RoundEnvironment roundEnv, ProcessingEnvironment processingEnv,
    //                                   SetterStore store) {
    //        LibTypes libTypes = ModelAnalyzer.getInstance().libTypes;
    //        Class<? extends Annotation> inverseMethodClass = libTypes.getInverseMethodClass();
    //        for (Element element : AnnotationUtil
    //                .getElementsAnnotatedWith(roundEnv, inverseMethodClass)) {
    //            try {
    //                if (!element.getModifiers().contains(Modifier.PUBLIC)) {
    //                    L.e(element, "@InverseMethods must be associated with a public method");
    //                    continue;
    //                }
    //                ExecutableElement executableElement = (ExecutableElement) element;
    //                if (executableElement.getReturnType().getKind() == TypeKind.VOID) {
    //                    L.e(element, "@InverseMethods must have a non-void return type");
    //                    continue;
    //                }
    //                final InverseMethodCompat inverseMethod =
    //                        InverseMethodCompat.create(executableElement);
    //                final String target = inverseMethod.getValue();
    //                if (!StringUtils.isNotBlank(target)) {
    //                    L.e(element, "@InverseMethod must supply a value containing the name of " +
    //                            "the method to call when going from View value to bound value");
    //                    continue;
    //                }
    //                if (executableElement.getParameters().isEmpty()) {
    //                    L.e(element, "@InverseMethods must have at least one parameter.");
    //                    continue;
    //                }
    //                try {
    //                    ExecutableElement inverse = findInverseOf(processingEnv, executableElement,
    //                            inverseMethod.getValue());
    //                    store.addInverseMethod(processingEnv, executableElement, inverse);
    //                } catch (IllegalArgumentException e) {
    //                    L.e(element, "%s", e.getMessage());
    //                }
    //            } catch (LoggedErrorException e) {
    //                // This will be logged later
    //            }
    //        }
    //    }
    //    private ExecutableElement findInverseOf(ProcessingEnvironment env, ExecutableElement method,
    //                                            String name) throws IllegalArgumentException {
    //        TypeElement enclosingType = (TypeElement) method.getEnclosingElement();
    //        List<? extends VariableElement> params = method.getParameters();
    //        Types typeUtil = env.getTypeUtils();
    //        for (Element element : env.getElementUtils().getAllMembers(enclosingType)) {
    //            if (element.getKind() == ElementKind.METHOD) {
    //                ExecutableElement executableElement = (ExecutableElement) element;
    //                if (!name.equals(executableElement.getSimpleName().toString())) {
    //                    continue;
    //                }
    //
    //                List<? extends VariableElement> checkParams = executableElement.getParameters();
    //                boolean allTypesMatch = true;
    //                // now check the parameters
    //                for (int i = 0; i < params.size() - 1; i++) {
    //                    TypeMirror expectedType = typeUtil.erasure(params.get(i).asType());
    //                    TypeMirror foundType = typeUtil.erasure(checkParams.get(i).asType());
    //                    if (!typeUtil.isSameType(expectedType, foundType)) {
    //                        allTypesMatch = false;
    //                        break;
    //                    }
    //                }
    //                if (allTypesMatch) {
    //                    TypeMirror expectedType = typeUtil.erasure(method.getReturnType());
    //                    TypeMirror foundType =
    //                            typeUtil.erasure(checkParams.get(checkParams.size() - 1).asType());
    //                    allTypesMatch = typeUtil.isSameType(expectedType, foundType);
    //                    if (allTypesMatch) {
    //                        // check return type
    //                        expectedType =
    //                                typeUtil.erasure(params.get(params.size() - 1).asType());
    //                        foundType = typeUtil.erasure(executableElement.getReturnType());
    //                        if (!typeUtil.isSameType(expectedType, foundType)) {
    //                            throw new IllegalArgumentException(String.format(
    //                                    "Declared InverseMethod ('%s') does not have the correct " +
    //                                            "return type. Expected '%s' but was '%s'",
    //                                    executableElement, expectedType, foundType));
    //                        }
    //                    }
    //                    if (method.getModifiers().contains(Modifier.STATIC) !=
    //                            executableElement.getModifiers().contains(Modifier.STATIC)) {
    //                        throw new IllegalArgumentException(String.format(
    //                                "'%s' declared instance method is different from its " +
    //                                        "InverseMethod '%s'. Make them both static or instance " +
    //                                        "methods.", method, executableElement));
    //                    }
    //                    if (!executableElement.getModifiers().contains(Modifier.PUBLIC)) {
    //                        throw new IllegalArgumentException(String.format(
    //                                "InverseMethod must be declared public '%s'", executableElement));
    //                    }
    //                    return executableElement;
    //                }
    //            }
    //        }
    //        StringBuilder paramStr = new StringBuilder();
    //        for (int i = 0; i < params.size() - 1; i++) {
    //            if (i != 0) {
    //                paramStr.append(", ");
    //            }
    //            paramStr.append(params.get(i).asType());
    //        }
    //        if (params.size() != 1) {
    //            paramStr.append(", ");
    //        }
    //        paramStr.append(method.getReturnType());
    //        String staticStr = method.getModifiers().contains(Modifier.STATIC) ? "static " : "";
    //        TypeMirror returnType = params.get(params.size() - 1).asType();
    //        throw new IllegalArgumentException(String.format(
    //                "Could not find inverse method: public %s%s %s(%s)", staticStr, returnType,
    //                name, paramStr));
    //    }
    //    private void addUntaggable(RoundEnvironment roundEnv, SetterStore store) {
    //        LibTypes libTypes = ModelAnalyzer.getInstance().libTypes;
    //        Class<? extends Annotation> untaggableClass = libTypes.getUntaggableClass();
    //        for (Element element : AnnotationUtil.
    //                getElementsAnnotatedWith(roundEnv, untaggableClass)) {
    //            try {
    //                UntaggableCompat untaggable = UntaggableCompat.create(element);
    //                store.addUntaggableTypes(untaggable.getValue(), (TypeElement) element);
    //            } catch (LoggedErrorException e) {
    //                // This will be logged later
    //            }
    //        }
    //    }
    private fun clearIncrementalClasses(roundEnv: Resolver, store: SetterStore) {
        val classes = HashSet<String>()
        val libTypes = getInstance().libTypes
        for (element in getElementsAnnotatedWith(roundEnv, libTypes.bindingAdapterClass.name)) {
            val containingClass = element.parentDeclaration
            classes.add(containingClass!!.qualifiedName!!.asString())
        }
        for (element in getElementsAnnotatedWith(roundEnv, libTypes.bindingMethodsClass.name)) {
            classes.add(element.qualifiedName!!.asString())
        }
        for (element in getElementsAnnotatedWith(roundEnv, libTypes.bindingConversionClass.name)) {
            val containingClass = element.parentDeclaration
            classes.add(containingClass!!.qualifiedName!!.asString())
        }
        for (element in getElementsAnnotatedWith(roundEnv, libTypes.untaggableClass.name)) {
            classes.add(element.qualifiedName!!.asString())
        }
        store.clear(classes)
    }

}
