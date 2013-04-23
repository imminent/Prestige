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
package com.imminentmeals.prestige.example.models.implementations;

import static com.imminentmeals.prestige.annotations.meta.Implementations.DEVELOPMENT;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.imminentmeals.prestige.annotations.ModelImplementation;
import com.imminentmeals.prestige.example.models.NewsCategory;
import com.imminentmeals.prestige.example.models.NewsSource;

/**
 * Source of strange and wonderful news.
 *
 * This singleton functions as the repository for the news we display.
 */
@ModelImplementation(DEVELOPMENT)
@Singleton
public class FakeNewsSource implements NewsSource {
    // the category names
    final String[] CATEGORIES = { "Top Stories", "US", "Politics", "Economy" };

    // category objects, representing each category
    NewsCategory[] category;

    @Inject
    /* package */FakeNewsSource() {
        int i;
        category = new NewsCategory[CATEGORIES.length];
        for (i = 0; i < CATEGORIES.length; i++)
            category[i] = new NewsCategory();
    }

    @Override
    public String[] categories() {
        return CATEGORIES;
    }

    @Override
    public NewsCategory categoryForIndex(int index) {
        return category[index];
    }
}
