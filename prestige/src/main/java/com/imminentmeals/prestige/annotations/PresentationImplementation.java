package com.imminentmeals.prestige.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifier;

import android.app.Activity;

/**
 * <p>Marks the {@link Activity} as a Presentation implementation.</p>
 * @author Dandre Allison
 */
@Inherited
@Documented
@TypeQualifier(applicableTo = Activity.class)
@Retention(RetentionPolicy.SOURCE)
public @interface PresentationImplementation { }
