/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.imminentmeals.prestige.example.presentations.framework;

import android.app.ActionBar.OnNavigationListener;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.FragmentTransaction;

/**
 * Adapter for action bar navigation events.
 *
 * This class implements an adapter that facilitates handling of action bar navigation events. An
 * instance of this class must be installed as a TabListener or OnNavigationListener on an Action
 * Bar, and it will relay the navigation events to a configured listener.
 */
public class ActionBarNavigationObserver implements TabListener, OnNavigationListener {

  /**
   * Constructs an instance with the given listener.
   *
   * @param navigation_callback the listener to notify when a navigation event occurs.
   */
  public ActionBarNavigationObserver(NewsReaderNavigationCallback navigation_callback) {
    this._navigation_callback = navigation_callback;
  }

  /**
   * Called by framework when a tab is selected.
   *
   * This will cause a navigation event to be delivered to the configured listener.
   */
  @Override public void onTabSelected(Tab tab, FragmentTransaction _) {
    _navigation_callback.onCategorySelected(tab.getPosition());
  }

  /**
   * Called by framework when a item on the navigation menu is selected.
   *
   * This will cause a navigation event to be delivered to the configured listener.
   */
  @Override public boolean onNavigationItemSelected(int index, long _) {
    _navigation_callback.onCategorySelected(index);
    return true;
  }

  /**
   * Called by framework when a tab is re-selected. That is, it was already selected and is tapped
   * on again. This is not used in our app.
   */
  @Override public void onTabReselected(Tab _, FragmentTransaction __) {
    // we don't care
  }

  /**
   * Called by framework when a tab is unselected. Not used in our app.
   */
  @Override public void onTabUnselected(Tab _, FragmentTransaction __) {
    // we don't care
  }

  /** The callback to trigger on navigation events */
  private NewsReaderNavigationCallback _navigation_callback;
}
