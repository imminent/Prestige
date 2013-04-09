package com.imminentmeals.prestige.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Marks the class as a Controller and defines the Presentation it controls.</p>
 * @author Dandre Allison
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Controller {
    Class<?> presentation() default Default.class;
    
    public static final class Default { } 
}
