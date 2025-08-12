package android.databinding.tool.reflection.annotation.ksp

import android.databinding.tool.ksp.isDeclaredType
import android.databinding.tool.reflection.ModelClass
import android.databinding.tool.reflection.ModelField
import android.databinding.tool.reflection.ModelMethod
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ksp.toTypeName

public class KspAnnotationClass(
    @JvmField
    val typeMirror: KSType
) : ModelClass() {

    override val isArray: Boolean
        get() = TODO("Not yet implemented")
    override val componentType: ModelClass?
        get() = TODO("Not yet implemented")
    override val isNullable: Boolean
        get() = TODO("Not yet implemented")
    override val isPrimitive: Boolean
        get() = TODO("Not yet implemented")
    override val isBoolean: Boolean
        get() = TODO("Not yet implemented")
    override val isChar: Boolean
        get() = TODO("Not yet implemented")
    override val isByte: Boolean
        get() = TODO("Not yet implemented")
    override val isShort: Boolean
        get() = TODO("Not yet implemented")
    override val isInt: Boolean
        get() = TODO("Not yet implemented")
    override val isLong: Boolean
        get() = TODO("Not yet implemented")
    override val isFloat: Boolean
        get() = TODO("Not yet implemented")
    override val isDouble: Boolean
        get() = TODO("Not yet implemented")


    override val isGeneric by lazy(LazyThreadSafetyMode.NONE) {
        typeMirror.declaration.isDeclaredType() && typeMirror.arguments.isNotEmpty()
    }

    override val typeArguments by lazy(LazyThreadSafetyMode.NONE) {
        if (typeMirror.declaration.isDeclaredType()) {
            typeMirror.arguments?.map {
                it.type?.resolve()?.let { kstype ->
                    KspAnnotationClass(kstype)
                } ?:run {
                  throw RuntimeException("create KspAnnotationClass with null ks type!!")
                }

            }?.let {
                it.ifEmpty {
                    null
                }
            }
        } else {
            null
        }
    }
    override val isTypeVar: Boolean
        get() = TODO("Not yet implemented")
    override val isWildcard: Boolean
        get() = TODO("Not yet implemented")
    override val isInterface: Boolean
        get() = TODO("Not yet implemented")
    override val isVoid: Boolean
        get() = TODO("Not yet implemented")

    override val superclass by lazy(LazyThreadSafetyMode.NONE) {
        val superClass: KSType? = if (typeMirror.declaration.isDeclaredType()) {
            // Should be able to do without resolution
            (typeMirror as KSClassDeclaration).superTypes
                .map { it.resolve() }
                .filter { (it?.declaration as? KSClassDeclaration)?.classKind == ClassKind.CLASS }
                .first()
        } else {
            null
        }
        if (superClass?.declaration?.isDeclaredType() == true) {
            KspAnnotationClass(superClass)
        } else {
            null
        }
    }

    override val jniDescription: String
        get() = TODO("Not yet implemented")


    override val allFields: List<ModelField>
        get() = TODO("Not yet implemented")


    override val allMethods by lazy(LazyThreadSafetyMode.NONE) {
        if (typeMirror.declaration.isDeclaredType()) {
            (typeMirror.declaration as KSClassDeclaration).getAllFunctions().map { ksFunction ->
                KspAnnotationMethod(typeMirror, ksFunction)
            }.toList()
        } else {
            emptyList()
        }
    }


    override fun toJavaCode(): String {
        TODO("Not yet implemented")
    }

    override fun unbox(): ModelClass {
        TODO("Not yet implemented")
    }

    override fun box(): ModelClass {
        TODO("Not yet implemented")
    }


    override fun equals(other: Any?): Boolean {
        if (other is KspAnnotationClass) {
            return typeMirror.toTypeName() == other.typeMirror.toTypeName()
        }
        return false
    }

    override fun isAssignableFrom(that: ModelClass?): Boolean {
        var other: ModelClass? = that
        while (other != null && other !is KspAnnotationClass) {
            other = other.superclass
        }
        if (other == null) {
            return false
        }
        if (equals(other)) {
            return true
        }
        val thatAnnotationClass = other as? KspAnnotationClass ?: return false

        if (this.typeMirror.isAssignableFrom(thatAnnotationClass.typeMirror)) {
            return true
        }


        //TODO ksp

        // If this is incomplete, java typeUtils won't be able to detect assignments like
        // List <- List<String> because we'll resolve List as List<T>.
        // To handle those cases, we run a custom assignability as well :/
//        if (isIncomplete || other.isIncomplete) {
//            if (this.isTypeVar) {
//                // if this is a type var and resolved as a type var, accept it.
//                // This allows assigning List<Foo> to List (List<?> is internal representation so
//                // technically it is assigning Foo to ?)
//                // Java does also accept assigning List<?> to List<Foo> but we'll not accept it
//                // as it creates really weird assignability cases like ? being assignable to
//                // LiveData if ? is resolved from a generic. For instance:
//                // data class Foo<T>(val value : T)
//                // the type of `foo.value` will be `?` so checking LiveData isAssignableFrom
//                // `foo.value` would return true (which we don't want).
//                return true
//            }
//            val myTypeArguments = typeArguments ?: return false
//            val otherTypeArguments = other.typeArguments ?: return false
//            val myErasure = erasure()
//            val otherErasure = other.erasure()
//            if (myTypeArguments.size == otherTypeArguments.size &&
//                myErasure.isAssignableFrom(otherErasure)) {
//                myTypeArguments.forEachIndexed { index, myTypeArgument ->
//                    if (!myTypeArgument.isAssignableFrom(otherTypeArguments[index])) {
//                        return false
//                    }
//                }
//                return true
//            }
//        }
        return false
    }

    override fun erasure(): ModelClass = computedErasure

    private val computedErasure by lazy(LazyThreadSafetyMode.NONE) {
        val erasure = typeMirror.starProjection()
        if (erasure === typeMirror) {
            this
        } else {
            KspAnnotationClass(erasure)
        }
    }
}