package com.imminentmeals.prestige.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Marks the class as a Presentation that a Presentation's Controller can use to interact
 * with the Presentation and defines the Protocol a Controller must meet the specifications of to 
 * qualify as a valid Controller for the given Presentation. Notice that it is valid for a Presentation not to 
 * require a Protocol.</p>
 * @author Dandre Allison
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Presentation {
	Class<?> protocol() default NoProtocol.class;
    
    public interface NoProtocol { }
}
