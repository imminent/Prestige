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
		
		_segue_controller = Prestige.materialize(this, Implementations.PRODUCTION);
	}
	
/* SegueControllerApplication Contract */
	@Override
	public SegueController segueController() {
		return _segue_controller;
	}

	private SegueController _segue_controller;
}
