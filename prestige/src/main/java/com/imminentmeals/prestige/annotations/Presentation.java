package com.imminentmeals.prestige.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifier;

import android.app.Activity;

/**
 * <p>Marks the class as a Presentation and defines the Protocol a Controller must meet the specifications of to 
 * qualify as a valid Controller for the given Presentation. Notice that it is valid for a Presentation not to 
 * require a Protocol.</p>
 * @author Dandre Allison
 */
@Documented
@TypeQualifier(applicableTo = Activity.class)
@Retention(RetentionPolicy.SOURCE)
public @interface Presentation {
    Class<?> protocol() default NoProtocol.class;
    
    public interface NoProtocol { }
}
