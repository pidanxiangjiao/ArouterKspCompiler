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
package android.databinding.tool.reflection.annotation.ksp;

import android.databinding.tool.LibTypes;
import android.databinding.tool.reflection.ImportBag;
import android.databinding.tool.reflection.ModelAnalyzer;
import android.databinding.tool.reflection.ModelClass;
import android.databinding.tool.reflection.TypeUtil;
import android.databinding.tool.util.L;

import com.google.devtools.ksp.processing.Resolver;
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment;
import com.google.devtools.ksp.symbol.KSClassDeclaration;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSTypeArgument;
import com.google.devtools.ksp.symbol.Variance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class KspAnnotationAnalyzer extends ModelAnalyzer {

    public final Map<String, KSType> PRIMITIVE_TYPES;

    public final SymbolProcessorEnvironment mKspProcessingEnv;
    public final Resolver mResolver;

    public KspAnnotationAnalyzer(Resolver resolver, SymbolProcessorEnvironment processingEnvironment, LibTypes libTypes) {
        super(libTypes);
        this.mKspProcessingEnv = processingEnvironment;
        this.mResolver = resolver;

        PRIMITIVE_TYPES = new HashMap<String, KSType>();
        PRIMITIVE_TYPES.put("boolean", mResolver.getBuiltIns().getBooleanType());
        PRIMITIVE_TYPES.put("byte", mResolver.getBuiltIns().getByteType());
        PRIMITIVE_TYPES.put("short", mResolver.getBuiltIns().getShortType());
        PRIMITIVE_TYPES.put("char", mResolver.getBuiltIns().getCharType());
        PRIMITIVE_TYPES.put("int", mResolver.getBuiltIns().getIntType());
        PRIMITIVE_TYPES.put("long", mResolver.getBuiltIns().getLongType());
        PRIMITIVE_TYPES.put("float", mResolver.getBuiltIns().getFloatType());
        PRIMITIVE_TYPES.put("double", mResolver.getBuiltIns().getDoubleType());
        PRIMITIVE_TYPES.put("void", mResolver.getBuiltIns().getUnitType());
    }

    public static KspAnnotationAnalyzer get() {
        return (KspAnnotationAnalyzer) getInstance();
    }

    @Override
    public KspAnnotationClass loadPrimitive(String className) {
        KSType typeKind = PRIMITIVE_TYPES.get(className);
        if (typeKind == null) {
            return null;
        } else {
            return new KspAnnotationClass(typeKind);
        }
    }

    @Override
    public ModelClass findClassInternal(String className, ImportBag imports) {
        Resolver typeUtils = getKspResolver();
        className = className.trim();
        int numDimensions = 0;
        while (className.endsWith("[]")) {
            numDimensions++;
            className = className.substring(0, className.length() - 2);
        }
        KspAnnotationClass primitive = loadPrimitive(className);
        if (primitive != null) {
            return addDimension(primitive.typeMirror, numDimensions);
        }
        if ("void".equals(className)) {
            return addDimension(mResolver.getBuiltIns().getUnitType(), numDimensions);
        }
        int templateOpenIndex = className.indexOf('<');
        KSType declaredType;
        if (templateOpenIndex < 0) {
            KSClassDeclaration typeElement = getTypeElement(className, imports);
            if (typeElement == null) {
                return null;
            }
            declaredType = typeElement.asStarProjectedType();
        } else {
            int templateCloseIndex = className.lastIndexOf('>');
            String paramStr = className.substring(templateOpenIndex + 1, templateCloseIndex);

            String baseClassName = className.substring(0, templateOpenIndex);
            KSClassDeclaration typeElement = getTypeElement(baseClassName, imports);
            if (typeElement == null) {
                L.e("cannot find type element for %s", baseClassName);
                return null;
            }

            ArrayList<String> templateParameters = splitTemplateParameters(paramStr);
//            KSType[] typeArgs = new KSType[templateParameters.size()];
            List<KSTypeArgument> typeArguments = new ArrayList<>();
            for (int i = 0; i < templateParameters.size(); i++) {
                final KspAnnotationClass clazz = (KspAnnotationClass)
                        findClass(templateParameters.get(i), imports);
                if (clazz == null) {
                    L.e("cannot find type argument for %s in %s", templateParameters.get(i),
                            baseClassName);
                    return null;
                }
//                typeArgs[i] = clazz.typeMirror;
                typeArguments.add(typeUtils.getTypeArgument(typeUtils.createKSTypeReferenceFromKSType(clazz.typeMirror), Variance.INVARIANT));
            }
            declaredType = typeElement.asType(typeArguments);
        }
        return addDimension(declaredType, numDimensions);
    }

    private KspAnnotationClass addDimension(KSType type, int numDimensions) {
        while (numDimensions > 0) {
            List<KSTypeArgument> types = new ArrayList<KSTypeArgument>();
            types.add(getKspResolver().getTypeArgument(getKspResolver().createKSTypeReferenceFromKSType(type), Variance.INVARIANT));
            type = getKspResolver().getBuiltIns().getArrayType().replace(types);
            numDimensions--;
        }
        return new KspAnnotationClass(type);
    }

    private KSClassDeclaration getTypeElement(String className, ImportBag imports) {
        Resolver elementUtils = getKspResolver();
        final boolean hasDot = className.indexOf('.') >= 0;
        if (!hasDot && imports != null) {
            // try the imports
            String importedClass = imports.find(className);
            if (importedClass != null) {
                className = importedClass;
            }
        }
        if (className.indexOf('.') < 0) {
            // try java.lang.
            String javaLangClass = "java.lang." + className;
            try {
                KSClassDeclaration javaLang = elementUtils.getClassDeclarationByName(elementUtils.getKSNameFromString(javaLangClass));
                if (javaLang != null) {
                    return javaLang;
                }
            } catch (Exception e) {
                // try the normal way
            }
        }
        try {
            KSClassDeclaration typeElement = elementUtils.getClassDeclarationByName(elementUtils.getKSNameFromString(className));
            if (typeElement == null && hasDot && imports != null) {
                int lastDot = className.lastIndexOf('.');
                KSClassDeclaration parent = getTypeElement(className.substring(0, lastDot), imports);
                if (parent != null) {
                    String name = parent.getQualifiedName() + "."
                            + className.substring(lastDot + 1);
                    return getTypeElement(name, null);
                }
            }
            // try to jetify if we couldn't find it
            if (typeElement == null) {
                String converted = libTypes.convert(className);
                if (!converted.equals(className)) {
                    return getTypeElement(converted, imports);
                }
            }
            return typeElement;
        } catch (Exception e) {
            return null;
        }
    }

    private ArrayList<String> splitTemplateParameters(String templateParameters) {
        ArrayList<String> list = new ArrayList<>();
        int index = 0;
        int openCount = 0;
        StringBuilder arg = new StringBuilder();
        while (index < templateParameters.length()) {
            char c = templateParameters.charAt(index);
            if (c == ',' && openCount == 0) {
                list.add(arg.toString());
                arg.delete(0, arg.length());
            } else if (!Character.isWhitespace(c)) {
                arg.append(c);
                if (c == '<') {
                    openCount++;
                } else if (c == '>') {
                    openCount--;
                }
            }
            index++;
        }
        list.add(arg.toString());
        return list;
    }

    @Override
    public ModelClass findClass(Class classType) {
        return findClass(classType.getCanonicalName(), null);
    }

    public Resolver getKspResolver() {
        return mResolver;
    }

    @Override
    public TypeUtil createTypeUtil() {
        return new KspAnnotationTypeUtil(this);
    }

    @Override
    protected boolean findGeneratedAnnotation() {
        return mResolver.getClassDeclarationByName(
                mResolver.getKSNameFromString(GENERATED_ANNOTATION)) != null;
    }
}
