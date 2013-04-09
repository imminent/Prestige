package com.imminentmeals.prestige.annotations;

import static com.imminentmeals.prestige.annotations.meta.Implementations.PRODUCTION;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifierNickname;

/**
 *
 * @author Dandre Allison
 */
@Documented
@TypeQualifierNickname
@ControllerImplementation(PRODUCTION)
@Retention(RetentionPolicy.SOURCE)
public @interface ProductionController {
}
