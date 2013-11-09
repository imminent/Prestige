package com.imminentmeals.prestige.example;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import butterknife.Views;
import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class InjectionCallbacks implements Application.ActivityLifecycleCallbacks {

  @Override public void onActivityCreated(Activity activity, @CheckForNull Bundle _) {
    Views.inject(activity);
  }

  @Override public void onActivityStarted(Activity activity) {

  }

  @Override public void onActivityResumed(Activity activity) {

  }

  @Override public void onActivityPaused(Activity activity) {

  }

  @Override public void onActivityStopped(Activity activity) {

  }

  @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

  }

  @Override public void onActivityDestroyed(Activity activity) {

  }
}
