package com.imminentmeals.prestige.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Marks the class as a Presentation Fragment that a Presentation's Controller can use to interact
 * with the Presentation Fragment and defines the Protocol a Controller must meet the specifications of to 
 * qualify as a valid Controller for the Presentation that holds the Presentation Fragment. Notice that it 
 * is valid for a Presentation Fragment not to require a Protocol.</p>
 * @author Dandre Allison
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface PresentationFragment {
	Class<?> protocol() default NoProtocol.class;
    
    public interface NoProtocol { }
}
