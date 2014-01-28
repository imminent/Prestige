package com.imminentmeals.prestige.example;

import android.app.Application;
import com.imminentmeals.prestige.Prestige;
import com.imminentmeals.prestige.annotations.InjectModel;
import com.imminentmeals.prestige.example.models.DeveloperTools;
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
  @InjectModel /* package */DeveloperTools _tools;

/* Lifecycle */
	@Override public void onCreate() {
		super.onCreate();

		Prestige.materialize(this, scope());
    Prestige.injectModels(this);
    if (BuildConfig.DEBUG) {
      _tools.setupAndroidCodeWarnings();
      Timber.plant(new Timber.DebugTree());
    }
    _storage_model.selfStorageFacility(new SelfStorageFacility(this));

    registerActivityLifecycleCallbacks(new InjectionCallbacks());
	}

  /**
   * Hook that allows extensions of {@link ExampleApplication} to choose alternative scopes.
   * @return The implementation scope the application is using
   */
  protected String scope() {
    return _IMPLEMENTATION_SCOPE;
  }

  private static final String _IMPLEMENTATION_SCOPE = BuildConfig.DEBUG? DEVELOPMENT : PRODUCTION;
}
