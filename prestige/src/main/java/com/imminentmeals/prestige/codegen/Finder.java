package com.imminentmeals.prestige.codegen;

import com.imminentmeals.prestige.SegueControllerApplication;

import android.app.Activity;
import android.app.Fragment;

/**
 * <p>Defines the API for finding the {@link SegueControllerApplication} given an {@link Activity} or {@link Fragment}.</p>
 * @author Dandre Allison
 */
public enum Finder {
	/**
	 * <p>Finds the {@link SegueControllerApplication} given a {@link Fragment}.</p>
	 */
	FRAGMENT {
		@Override
		public SegueControllerApplication findSegueControllerApplication(Object source) {
			return ACTIVITY.findSegueControllerApplication(((Fragment) source).getActivity());
		}
	},
	/**
	 * <p>Finds the {@link SegueControllerApplication} given a {@link Activity}.</p>
	 */
	ACTIVITY {
		@Override
		public SegueControllerApplication findSegueControllerApplication(Object source) {
			return (SegueControllerApplication) ((Activity) source).getApplication();
		}
	};

	/** 
	 * <p>Finds the {@link SegueControllerApplication} given a valid source.</p> 
	 * @param source The source of the SegueControllerApplication
	 * @return The SegueControllerApplication
	 */
	public abstract SegueControllerApplication findSegueControllerApplication(Object source);
}