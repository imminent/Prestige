package com.imminentmeals.prestige.annotations;

import static com.imminentmeals.prestige.annotations.meta.Implementations.DEVELOPMENT;

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
@ControllerImplementation(DEVELOPMENT)
@Retention(RetentionPolicy.SOURCE)
public @interface DevelopmentController {
}
