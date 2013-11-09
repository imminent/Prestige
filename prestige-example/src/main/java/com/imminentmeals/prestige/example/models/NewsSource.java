package com.imminentmeals.prestige.example.models;

import com.imminentmeals.prestige.annotations.Model;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/**
 * @author Dandre Allison
 */
@Model
public interface NewsSource {
  /** Returns the list of news categories. */
  @Nonnull String[] categories();

  /** Returns a category by index. */
  @Nonnull NewsCategory categoryForIndex(@Nonnegative int index);
}
