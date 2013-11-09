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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Generator of random news. More fun than "lorem ipsum", isn't it?
 *
 * This generator can construct headlines and news articles by randomly composing sentences. Any
 * resemblance to actual events (or, actually, any resemblance to anything that makes sense) is
 * merely coincidental!
 */
@ParametersAreNonnullByDefault
public class NonsenseGenerator {

  public NonsenseGenerator() {
    _random = new Random();
  }

  /** Produces something that reads like a headline. */
  public String makeHeadline() {
    return makeSentence(true);
  }

  /**
   * Produces a sentence.
   *
   * @param isHeadline whether the sentence should look like a headline or not.
   * @return the generated sentence.
   */
  public String makeSentence(boolean isHeadline) {
    List<String> words = new ArrayList<>();
    generateSentence(words, isHeadline);
    words.set(0, String.valueOf(Character.toUpperCase(words.get(0).charAt(0)))
        + words.get(0).substring(1));
    return joinWords(words);
  }

  /**
   * Produces news article text.
   *
   * @param number_of_sentences how many sentences the text is to contain.
   * @return the generated text.
   */
  public String makeText(int number_of_sentences) {
    final StringBuilder string_builder = stringBuilder();
    while (number_of_sentences-- > 0) {
      string_builder.append(makeSentence(false)).append(_PERIOD);
      if (number_of_sentences > 0) string_builder.append(_SPACE);
    }
    try {
      return string_builder.toString();
    } finally {
      string_builder.setLength(0);
    }
  }

  /**
   * Generates a sentence.
   *
   * @param words the list of words to which the sentence will be appended.
   * @param is_headline whether the sentence must look like a headline or not.
   */
  private void generateSentence(List<String> words, boolean is_headline) {
    if (!is_headline && _random.nextInt(4) == 0) generateTimeClause(words);
    generateAgent(words, is_headline);
    generatePredicate(words, is_headline);
  }

  private void generateTimeClause(List<String> words) {
    if (_random.nextInt(2) == 0) words.add(pickOneOf(_RELATIVE_TIMES));
    else {
      words.add(pickOneOf("this", "last"));
      words.add(pickOneOf("Monday", "Tuesday", "Wednesday", "Thursday"));
      words.add(pickOneOf("morning", "afternoon", "evening"));
    }
  }

  private void generateAgent(List<String> words, boolean is_headline) {
    if (!is_headline) words.add(pickOneOf(_ARTICLES));
    if (_random.nextInt(3) != 0) words.add(pickOneOf(ADJECTIVES));
    words.add(pickOneOf(THINGS));
  }

  private void generatePredicate(List<String> words, boolean is_headline) {
    words.add(pickOneOf(is_headline ? VERBS_PRESENT : VERBS_PAST));
    if (!is_headline) words.add(pickOneOf(_ARTICLES));
    if (_random.nextInt(3) != 0) words.add(pickOneOf(ADJECTIVES));
    words.add(pickOneOf(THINGS));

    if (_random.nextInt(3) == 0) {
      words.add(is_headline? pickOneOf(_PRESENT_VERBS) : pickOneOf(_PAST_VERBS));
      if (!is_headline) words.add(pickOneOf(_ARTICLES));
      if (_random.nextInt(3) != 0) words.add(pickOneOf(ADJECTIVES));
      words.add(pickOneOf(THINGS));
    }
  }

  private String pickOneOf(String... options) {
    return options[_random.nextInt(options.length)];
  }

  private static String joinWords(List<String> words) {
    if (words.size() == 0) return "";
    final StringBuilder string_builder = stringBuilder();
    string_builder.append(words.get(0));
    for (int i = 1, size = words.size(); i < size; i++) {
      if (!startsWithComma(words.get(i))) string_builder.append(_SPACE);
      string_builder.append(words.get(i));
    }
    try {
      return string_builder.toString();
    } finally {
      string_builder.setLength(0);
    }
  }

  private static StringBuilder stringBuilder() {
    if (_string_builder.get() == null) _string_builder.set(new StringBuilder());
    return _string_builder.get();
  }

  private static boolean startsWithComma(String word) {
    final Matcher comma_matcher;
    if (_COMMA.get() == null) {
      comma_matcher = Pattern.compile(",.*").matcher("");
      _COMMA.set(comma_matcher);
    } else comma_matcher = _COMMA.get();
    return comma_matcher.reset(word).matches();
  }

  private final Random _random;
  private static final ThreadLocal<StringBuilder> _string_builder = new ThreadLocal<>();
  private static final ThreadLocal<Matcher> _COMMA = new ThreadLocal<>();
  private static final String[] THINGS = { "bottle", "bowl", "brick", "building"
      , "bunny", "cake", "car", "cat", "cup", "desk", "dog", "duck"
      , "elephant", "engineer", "fork", "glass", "griffon", "hat", "key", "knife", "lawyer"
      , "llama", "manual", "meat", "monitor", "mouse", "tangerine", "paper", "pear", "pen"
      , "pencil", "phone", "physicist", "planet", "potato", "road", "salad", "shoe", "slipper"
      , "soup", "spoon", "star", "steak", "table", "terminal", "treehouse", "truck"
      , "watermelon", "window" };
  private static final String[] ADJECTIVES = { "red", "green", "yellow", "gray", "solid", "fierce"
      , "friendly", "cowardly", "convenient", "foreign", "national", "tall"
      , "short", "metallic", "golden", "silver", "sweet", "nationwide", "competitive"
      , "stable", "municipal", "famous" };
  private static final String[] VERBS_PAST = { "accused", "threatened", "warned", "spoke to"
      , "has met with", "was seen in the company of", "advanced towards", "collapsed on"
      , "signed a partnership with", "was converted into", "became", "was authorized to sell"
      , "sold", "bought", "rented", "allegedly spoke to", "leased", "is now investing on"
      , "is expected to buy", "is expected to sell", "was reported to have met with"
      , "will work together with", "plans to cease fire against", "started a war with"
      , "signed a truce with", "is now managing", "is investigating" };
  private static final String[] VERBS_PRESENT = { "accuses", "threatens", "warns", "speaks to"
      , "meets with", "seen with", "advances towards", "collapses on"
      , "signs partnership with", "converts into", "becomes", "is authorized to sell"
      , "sells", "buys", "rents", "allegedly speaks to", "leases", "invests on"
      , "expected to buy", "expected to sell", "reported to have met with"
      , "works together with", "plans cease fire against", "starts war with"
      , "signs truce with", "now manages" };
  private static final String[] _ARTICLES = new String[] { "a", "the" };
  private static final String[] _RELATIVE_TIMES = new String[] { "today", "yesterday"
      , "this afternoon", "this morning", "last evening" };
  private static final String[] _PRESENT_VERBS = new String[] {", claims", ", says"};
  private static final String[] _PAST_VERBS = new String[] {", claimed", ", said", ", reported"};
  private static final String _PERIOD = ".";
  private static final String _SPACE = " ";
}
