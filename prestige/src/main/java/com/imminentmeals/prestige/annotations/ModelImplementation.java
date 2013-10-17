package com.imminentmeals.prestige.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.imminentmeals.prestige.annotations.meta.Implementations.PRODUCTION;

/**
 * <p>Marks a class as an implementation of a Model in the specified scope.</p>
 * @author Dandre Allison
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface ModelImplementation {
	String value() default PRODUCTION;
    boolean serialize() default true;
}
