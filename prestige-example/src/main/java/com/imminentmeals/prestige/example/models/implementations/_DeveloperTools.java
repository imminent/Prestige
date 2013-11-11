package com.imminentmeals.prestige.example.models.implementations;

import android.os.StrictMode;
import com.imminentmeals.prestige.annotations.ModelImplementation;
import com.imminentmeals.prestige.example.models.DeveloperTools;

@ModelImplementation(serialize = false)
/* package */class _DeveloperTools implements DeveloperTools {

  @Override public void setupAndroidCodeWarnings() {
    StrictMode.enableDefaults();
  }
}
