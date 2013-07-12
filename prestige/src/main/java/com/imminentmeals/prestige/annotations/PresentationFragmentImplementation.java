package com.imminentmeals.prestige.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifier;

import android.app.Fragment;

/**
 * <p>Marks the {@link Fragment} as a Presentation Fragment implementation.</p>
 * @author Dandre Allison
 */
@Inherited
@Documented
@TypeQualifier(applicableTo = Fragment.class)
@Retention(RetentionPolicy.SOURCE)
public @interface PresentationFragmentImplementation {
	int layout() default -1;
}
