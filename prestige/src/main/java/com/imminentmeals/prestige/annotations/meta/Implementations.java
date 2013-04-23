package com.imminentmeals.prestige.annotations.meta;

/**
 * <p>Basic implementation scope values. {@link #PRODUCTION} is the default implementation that is provided, and any other
 * scope can be provided on top of that to replace production implementations. You are not limited to the provided values,
 * they are provided merely for convenience.</p>
 * @author Dandre Allison
 */
public interface Implementations {
	/** Default implementation scope, and the one that provides production app implementations */
	String PRODUCTION = "production";
	String TEST = "test";
	String DEVELOPMENT = "development";
}
