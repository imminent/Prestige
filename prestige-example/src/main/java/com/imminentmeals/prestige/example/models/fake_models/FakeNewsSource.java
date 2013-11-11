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
package com.imminentmeals.prestige.example.models.fake_models;

import com.imminentmeals.prestige.annotations.ModelImplementation;
import com.imminentmeals.prestige.example.models.NewsCategory;
import com.imminentmeals.prestige.example.models.NewsSource;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import static com.imminentmeals.prestige.annotations.meta.Implementations.DEVELOPMENT;

/**
 * Source of strange and wonderful news.
 *
 * This singleton functions as the repository for the news we display.
 */
@ModelImplementation(DEVELOPMENT)
public class FakeNewsSource implements NewsSource {

  public FakeNewsSource() {
    _category = new NewsCategory[_CATEGORIES.length];
    for (int i = 0; i < _CATEGORIES.length; i++) _category[i] = new NewsCategory();
  }

  @Override
  @Nonnull public String[] categories() {
    return _CATEGORIES;
  }

  @Override
  @Nonnull public NewsCategory categoryForIndex(@Nonnegative int index) {
    return _category[index];
  }


  // category objects, representing each category
  private NewsCategory[] _category;
  // the category names
  private static final String[] _CATEGORIES = {"Top Stories", "US", "Politics", "Economy"};
}
