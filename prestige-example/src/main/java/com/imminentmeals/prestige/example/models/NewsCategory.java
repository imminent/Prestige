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
package com.imminentmeals.prestige.example.models;

import java.util.Arrays;
import java.util.Iterator;

/**
 * A news category (collection of articles).
 */
public class NewsCategory {

  /**
   * Create a news category.
   *
   * The articles are dynamically generated with fun and random nonsense.
   */
  public NewsCategory() {
    final NonsenseGenerator nonsense_generator = new NonsenseGenerator();
    _articles = new NewsArticle[_ARTICLES_PER_CATEGORY];
    for (int i = 0; i < _articles.length; i++)
      _articles[i] = new NewsArticle(nonsense_generator);
  }

  /** Returns how many articles exist in this category. */
  public int getArticleCount() {
    return _articles.length;
  }

  /** Gets a particular article by index. */
  public NewsArticle getArticle(int index) {
    return _articles[index];
  }

  public Iterable<String> getArticleHeadlines() {
    return new Iterable<String>() {

      @Override
      public Iterator<String> iterator() {
        return new Iterator<String>() {

          public void remove() {
          }

          public String next() {
            return _iterator.next().getHeadline();
          }

          public boolean hasNext() {
            return _iterator.hasNext();
          }

          private final Iterator<NewsArticle> _iterator = Arrays.asList(_articles).iterator();
        };
      }
    };
  }

  // array of our articles
  /* inner-class */final NewsArticle[] _articles;
  // how many articles?
  private static final int _ARTICLES_PER_CATEGORY = 20;
}
