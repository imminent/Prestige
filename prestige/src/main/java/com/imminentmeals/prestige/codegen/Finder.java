package com.imminentmeals.prestige.codegen;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;

/**
 * <p>Defines the API for finding the {@link android.app.FragmentManager} or {@link android.content.Context}
 * given an {@link Activity} or {@link Fragment}.</p>
 * @author Dandre Allison
 */
public enum Finder {
	/**
	 * <p>Finds the {@link android.app.FragmentManager} or {@link android.content.Context} given a {@link Fragment}.</p>
	 */
	FRAGMENT {
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
	 * <p>Finds the {@link android.app.FragmentManager} or {@link android.content.Context} given a {@link Activity}.</p>
	 */
	ACTIVITY {
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