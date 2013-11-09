package com.imminentmeals.prestige.example;

import android.app.Application;
import android.os.StrictMode;

import com.imminentmeals.prestige.Prestige;
import com.imminentmeals.prestige.annotations.InjectModel;
import com.imminentmeals.prestige.example.models.StorageModel;

import timber.log.Timber;

import static com.imminentmeals.prestige.annotations.meta.Implementations.DEVELOPMENT;
import static com.imminentmeals.prestige.annotations.meta.Implementations.PRODUCTION;

/**
 *
 * @author Dandre Allison
 */
public class ExampleApplication extends Application {
  @InjectModel /* package */StorageModel _storage_model;

/* Lifecycle */
	@Override
	public void onCreate() {
		if (BuildConfig.DEBUG)
            StrictMode.enableDefaults();
		super.onCreate();
		
		Prestige.materialize(this, _IMPLEMENTATION_SCOPE, _LOG);
        Prestige.injectModels(this);
        _storage_model.selfStorageFacility(new SelfStorageFacility(this));

    registerActivityLifecycleCallbacks(new InjectionCallbacks());
	}

    private static final String _IMPLEMENTATION_SCOPE = BuildConfig.DEBUG? DEVELOPMENT : PRODUCTION;
    private static final Timber _LOG = BuildConfig.DEBUG? Timber.DEBUG : Timber.PROD;
}
