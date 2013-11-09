package com.imminentmeals.prestige.example.presentations.framework;

/**
 * A listener that listens to navigation events.
 *
 * Represents a listener for navigation events delivered by {@link ActionBarNavigationObserver}.
 */
public interface NewsReaderNavigationCallback {
  /**
   * Signals that the given news category was selected.
   *
   * @param category_index the selected category's index.
   */
  public void onCategorySelected(int category_index);
}
