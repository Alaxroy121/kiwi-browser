package org.chromium.base.annotations;

import java.lang.annotation.*;

/** Stub for Chromium's @VisibleForTesting annotation. */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface VisibleForTesting {
    String otherwise() default "";
}
