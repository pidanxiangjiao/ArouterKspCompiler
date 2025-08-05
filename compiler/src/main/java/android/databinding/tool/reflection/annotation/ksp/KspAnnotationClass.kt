package android.databinding.tool.reflection.annotation.ksp

import android.databinding.tool.reflection.ModelClass
import android.databinding.tool.reflection.ModelField
import android.databinding.tool.reflection.ModelMethod
import com.google.devtools.ksp.symbol.KSType
import javax.lang.model.type.TypeMirror

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
    override val isGeneric: Boolean
        get() = TODO("Not yet implemented")
    override val typeArguments: List<ModelClass>?
        get() = TODO("Not yet implemented")
    override val isTypeVar: Boolean
        get() = TODO("Not yet implemented")
    override val isWildcard: Boolean
        get() = TODO("Not yet implemented")
    override val isInterface: Boolean
        get() = TODO("Not yet implemented")
    override val isVoid: Boolean
        get() = TODO("Not yet implemented")
    override val superclass: ModelClass?
        get() = TODO("Not yet implemented")
    override val jniDescription: String
        get() = TODO("Not yet implemented")
    override val allFields: List<ModelField>
        get() = TODO("Not yet implemented")
    override val allMethods: List<ModelMethod>
        get() = TODO("Not yet implemented")

    override fun toJavaCode(): String {
        TODO("Not yet implemented")
    }

    override fun unbox(): ModelClass {
        TODO("Not yet implemented")
    }

    override fun box(): ModelClass {
        TODO("Not yet implemented")
    }

    override fun isAssignableFrom(that: ModelClass?): Boolean {
        TODO("Not yet implemented")
    }

    override fun erasure(): ModelClass {
        TODO("Not yet implemented")
    }

}