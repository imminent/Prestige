package com.imminentmeals.prestige.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter.DEFAULT;

/**
 * <p>Marks the class as a Controller and defines the Presentation it controls.</p>
 * @author Dandre Allison
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {
    Class<?> presentation() default DEFAULT.class;
}
