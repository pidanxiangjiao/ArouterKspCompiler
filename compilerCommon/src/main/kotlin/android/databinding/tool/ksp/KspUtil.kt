
package android.databinding.tool.ksp


import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

@OptIn(KspExperimental::class)
internal inline fun <reified T : Annotation> KSAnnotated.findAnnotationWithType(): T? {
    return getAnnotationsByType(T::class).firstOrNull()
}

fun hasTypeVar(resolver: Resolver, type: KSType): Boolean {
    if (type.declaration is KSTypeParameter) {
        return true
    }

    val arrayType = resolver.builtIns.arrayType
    if (type == arrayType && type.arguments.isNotEmpty()) {
        val componentType = type.arguments.first().type?.resolve()
        if (componentType != null) {
            return hasTypeVar(resolver, componentType)
        }
    }

    if (type.arguments.isNotEmpty()) {
        for (arg in type.arguments) {
            val argType = arg.type?.resolve()
            if (argType != null && hasTypeVar(resolver, argType)) {
                return true
            }
        }
        return false
    }

    return false
}

fun eraseType(resolver: Resolver, type: KSType): KSType {
    return if (hasTypeVar(resolver, type)) {
        type.starProjection()
    } else {
        type
    }
}


fun getQualifiedName(type: KSType): String {

    val res = (type.declaration as? KSClassDeclaration)?.qualifiedName?.asString() ?:""

    return res
}


fun KSAnnotated.hasAnnotation(fqn: String): Boolean =
    annotations.any {
        fqn.endsWith(it.shortName.asString()) &&
                it.annotationType.resolve().declaration.qualifiedName?.asString() == fqn
    }

val JVM_STATIC_ANNOTATION_FQN = "kotlin.jvm.JvmStatic"
