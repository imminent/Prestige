package com.imminentmeals.prestige.example.models.test_models;

import com.imminentmeals.prestige.annotations.ModelImplementation;
import com.imminentmeals.prestige.example.models.DeveloperTools;

import static com.imminentmeals.prestige.annotations.meta.Implementations.TEST;

@ModelImplementation(value = TEST, serialize = false)
public class TestDeveloperTools implements DeveloperTools {

  @Override public void setupAndroidCodeWarnings() { }
}
