package com.imminentmeals.prestige.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Marks the field to be injected with the Presentation's Data Source.</p>
 * @author Dandre Allison
 */
@Target({ ElementType.FIELD, ElementType.CONSTRUCTOR })
@Retention(RetentionPolicy.SOURCE)
public @interface InjectModel { }
