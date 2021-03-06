package com.imminentmeals.prestige.annotations;

import static com.imminentmeals.prestige.annotations.meta.Implementations.PRODUCTION;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Marks a class as an implementation of a Controller for the specified scope.</p>
 * @author Dandre Allison
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface ControllerImplementation {
	String value() default PRODUCTION;
}
