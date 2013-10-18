package com.imminentmeals.prestige.example;

import android.app.Application;
import android.os.StrictMode;

import com.imminentmeals.prestige.Prestige;
import com.imminentmeals.prestige.annotations.meta.Implementations;

/**
 *
 * @author Dandre Allison
 */
public class ExampleApplication extends Application {

/* Lifecycle */
	@Override
	public void onCreate() {
		if (BuildConfig.DEBUG)
            StrictMode.enableDefaults();
		super.onCreate();
		
		Prestige.materialize(this, Implementations.DEVELOPMENT);
	}
}
