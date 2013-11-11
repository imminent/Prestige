package com.imminentmeals.prestige.example;

import com.imminentmeals.prestige.example.ExampleApplication;

import static com.imminentmeals.prestige.annotations.meta.Implementations.TEST;

public class TestExampleApplication extends ExampleApplication {

/* Scope hook */
  @Override public String scope() {
    return TEST;
  }
}
