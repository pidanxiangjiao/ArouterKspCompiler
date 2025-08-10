package androidx.databinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface BRFieldInfo {
    String name();
    int value();
}
