package com.imminentmeals.prestige.codegen;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;

import com.imminentmeals.prestige.SegueControllerApplication;

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
		
		@Override
		public FragmentManager findFragmentManager(Object source) {
			throw new IllegalArgumentException("Doesn't support nested Fragments yet.");
		}
		
		@Override
		public Context findContext(Object source) {
			return ((Fragment) source).getActivity();
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
		
		@Override
		public FragmentManager findFragmentManager(Object source) {
			return ((Activity) source).getFragmentManager();
		}
		
		@Override
		public Context findContext(Object source) {
			return (Activity) source;
		}
	};

	/** 
	 * <p>Finds the {@link SegueControllerApplication} given a valid source.</p> 
	 * @param source The source of the SegueControllerApplication
	 * @return The SegueControllerApplication
	 */
	public abstract SegueControllerApplication findSegueControllerApplication(Object source);
	
	/**
	 * <p>Finds the {@link FragmentManager} given a valid source.</p>
	 * @param source The source of the Fragment Manager
	 * @return The Fragment Manager
	 */
	public abstract FragmentManager findFragmentManager(Object source);
	
	/**
	 * <p>Finds the {@link Context} given a valid source.</p>
	 * @param source The source of the context
	 * @return The context
	 */
	public abstract Context findContext(Object source);
}