package com.imminentmeals.prestige;


/**
 * <p>Exception thrown to indicate that 
 * @author Dandre Allison
 */
public class IncompatiblePresentationException extends Exception {

	public IncompatiblePresentationException(Object presentation) {
		super("Attempting to attach imcompatible Presentation: " + presentation.getClass().getName() + " to a Controller.");
	}
	
	private static final long serialVersionUID = 4238214694659098583L;
}
