package com.imminentmeals.prestige.example;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.StrictMode;

import com.imminentmeals.prestige.Prestige;
import com.imminentmeals.prestige.SegueController;
import com.imminentmeals.prestige.SegueControllerApplication;
import com.imminentmeals.prestige.annotations.meta.Implementations;

/**
 *
 * @author Dandre Allison
 */
public class ExampleApplication extends Application implements SegueControllerApplication {

/* Lifecycle */
	@Override
	public void onCreate() {
		if (BuildConfig.DEBUG)
            StrictMode.enableDefaults();
		super.onCreate();
		
		_segue_controller = Prestige.conjureSegueController(Implementations.PRODUCTION);
		registerActivityLifecycleCallbacks(new PrestigeCallbacks());
	}
	
/* SegueControllerApplication Contract */
	@Override
	public SegueController segueController() {
		return _segue_controller;
	}

	private SegueController _segue_controller;
	
	private static class PrestigeCallbacks implements ActivityLifecycleCallbacks {

		@Override
		public void onActivityCreated(Activity activity, Bundle _) {
			Prestige.conjureController(activity);
		}

		@Override
		public void onActivityDestroyed(Activity activity) {
			Prestige.vanishController(activity);
		}

		@Override
		public void onActivityPaused(Activity _) { }

		@Override
		public void onActivityResumed(Activity _) { }

		@Override
		public void onActivitySaveInstanceState(Activity _, Bundle __) { }

		@Override
		public void onActivityStarted(Activity _) { }

		@Override
		public void onActivityStopped(Activity _) { }
	}
}
