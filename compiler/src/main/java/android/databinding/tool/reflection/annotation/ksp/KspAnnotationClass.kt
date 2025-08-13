package android.databinding.tool.reflection.annotation.ksp

import android.databinding.tool.ksp.getQualifiedName
import android.databinding.tool.ksp.isDeclaredType
import android.databinding.tool.ksp.isInterface
import android.databinding.tool.reflection.ModelAnalyzer
import android.databinding.tool.reflection.ModelClass
import android.databinding.tool.reflection.ModelField
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Variance
import com.squareup.kotlinpoet.ksp.toTypeName

public class KspAnnotationClass(
    @JvmField
    val typeMirror: KSType
) : ModelClass() {

    override val isArray: Boolean
        get() = typeMirror == (ModelAnalyzer.getInstance() as KspAnnotationAnalyzer).kspResolver.builtIns.arrayType

    override val componentType: ModelClass?
        get() = TODO("Not yet implemented")

    override val isNullable: Boolean
        get() = typeMirror.nullability == Nullability.NULLABLE

    override val isPrimitive: Boolean = false

    override val isBoolean: Boolean
        get() = typeMirror == (ModelAnalyzer.getInstance() as KspAnnotationAnalyzer).PRIMITIVE_TYPES["boolean"]

    override val isChar: Boolean
        get() = typeMirror == (ModelAnalyzer.getInstance() as KspAnnotationAnalyzer).PRIMITIVE_TYPES["char"]

    override val isByte: Boolean
        get() = typeMirror == (ModelAnalyzer.getInstance() as KspAnnotationAnalyzer).PRIMITIVE_TYPES["byte"]

    override val isShort: Boolean
        get() = typeMirror == (ModelAnalyzer.getInstance() as KspAnnotationAnalyzer).PRIMITIVE_TYPES["short"]

    override val isInt: Boolean
        get() = typeMirror == (ModelAnalyzer.getInstance() as KspAnnotationAnalyzer).PRIMITIVE_TYPES["int"]

    override val isLong: Boolean
        get() = typeMirror == (ModelAnalyzer.getInstance() as KspAnnotationAnalyzer).PRIMITIVE_TYPES["long"]

    override val isFloat: Boolean
        get() = typeMirror == (ModelAnalyzer.getInstance() as KspAnnotationAnalyzer).PRIMITIVE_TYPES["float"]

    override val isDouble: Boolean
        get() = typeMirror == (ModelAnalyzer.getInstance() as KspAnnotationAnalyzer).PRIMITIVE_TYPES["double"]


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
        get() = typeMirror.declaration is KSTypeParameter

    override val isWildcard: Boolean
        get() = typeMirror is KSTypeArgument && (typeMirror as KSTypeArgument).variance == Variance.STAR

    override val isInterface by lazy(LazyThreadSafetyMode.NONE) {
        typeMirror.declaration.isInterface()
    }

    override val isVoid: Boolean
        get() = typeMirror == (ModelAnalyzer.getInstance() as KspAnnotationAnalyzer).kspResolver.builtIns.unitType

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
        return getQualifiedName(typeMirror)
    }

    override fun unbox(): ModelClass {
        return this
    }

    override fun box(): ModelClass {
        return this
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