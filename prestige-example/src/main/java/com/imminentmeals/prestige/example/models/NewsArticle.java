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

/**
 * A news article.
 *
 * An article consists of a headline and a body. In this example app, article text is dynamically
 * generated nonsense.
 */
public class NewsArticle {

  /**
   * Create a news article with randomly generated text.
   *
   * @param nonsense_generator the nonsense generator to use.
   */
  public NewsArticle(NonsenseGenerator nonsense_generator) {
    _headline = nonsense_generator.makeHeadline();

    final StringBuilder string_builder = stringBuilder();
    string_builder.append(_ARTICLE_OPENER);
    string_builder.append(_HEADER_OPENER).append(_headline).append(_HEADER_CLOSER);
    for (int i = 0; i < _PARAGRAPHS_PER_ARTICLE; i++)
      string_builder.append(_PARAGRAPH_OPENER)
                    .append(nonsense_generator.makeText(_SENTENCES_PER_PARAGRAPH))
                    .append(_PARAGRAPH_CLOSER);

    string_builder.append(_ARTICLE_CLOSER);
    _body = string_builder.toString();
    string_builder.setLength(0);
  }

  /** Returns the headline. */
  public String getHeadline() {
    return _headline;
  }

  /** Returns the article body (HTML) */
  public String getBody() {
    return _body;
  }

  private static StringBuilder stringBuilder() {
    if (_string_builder.get() == null) _string_builder.set(new StringBuilder());
    return _string_builder.get();
  }

  // Headline and body
  private String _headline, _body;
  // How many sentences in each paragraph?
  private static final int _SENTENCES_PER_PARAGRAPH = 20;
  // How many paragraphs in each article?
  private static final int _PARAGRAPHS_PER_ARTICLE = 5;
  private static final ThreadLocal<StringBuilder> _string_builder = new ThreadLocal<>();
  private static final String _ARTICLE_OPENER = "<html><body>";
  private static final String _ARTICLE_CLOSER = "</body></html>";
  private static final String _HEADER_OPENER = "<h1>";
  private static final String _HEADER_CLOSER = "</h1>";
  private static final String _PARAGRAPH_OPENER = "<p>";
  private static final String _PARAGRAPH_CLOSER = "</p>";
}
