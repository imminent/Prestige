package com.imminentmeals.prestige.example.models;

import com.imminentmeals.prestige.annotations.Model;

/**
 *
 * @author Dandre Allison
 */
@Model
public interface NewsSource {
	/** Returns the list of news categories. */
    String[] categories();

    /** Returns a category by index. */
    NewsCategory categoryForIndex(int index);
}
