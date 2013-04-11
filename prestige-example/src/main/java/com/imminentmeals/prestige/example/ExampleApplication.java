package com.imminentmeals.prestige.example;

import android.app.Application;
import android.os.StrictMode;

import com.imminentmeals.prestige.SegueController;
import com.imminentmeals.prestige.SegueControllerApplication;

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
		
		_segue_controller = new SegueController();
	}
	
	@Override
	public Object segueController() {
		return _segue_controller;
	}

	private SegueController _segue_controller;
}
