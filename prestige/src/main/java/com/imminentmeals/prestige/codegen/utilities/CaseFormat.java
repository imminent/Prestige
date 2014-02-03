package com.imminentmeals.prestige.codegen.utilities;

import static com.imminentmeals.prestige.codegen.utilities.Preconditions.checkNotNull;

/**
 * Utility class for converting between various ASCII case formats.
 *
 * @author Mike Bostock
 * @since 1.0
 */
public enum CaseFormat {

  /**
   * C++ variable naming convention, e.g., "lower_underscore".
   */
  LOWER_UNDERSCORE(CharMatcher.is('_'), "_") {
    @Override String normalizeWord(String word) {
      return toLowerCase(word);
    }

    @Override String convert(CaseFormat format, String s) {
      return super.convert(format, s);
    }
  },

  /**
   * Java variable naming convention, e.g., "lowerCamel".
   */
  LOWER_CAMEL(CharMatcher.inRange('A', 'Z'), "") {
    @Override String normalizeWord(String word) {
      return firstCharOnlyToUpper(word);
    }
  },

  /**
   * Java and C++ class naming convention, e.g., "UpperCamel".
   */
  UPPER_CAMEL(CharMatcher.inRange('A', 'Z'), "") {
    @Override String normalizeWord(String word) {
      return firstCharOnlyToUpper(word);
    }
  };

  private final CharMatcher _word_boundary;
  private final String _word_separator;

  CaseFormat(CharMatcher word_boundary, String word_separator) {
    _word_boundary = word_boundary;
    _word_separator = word_separator;
  }

  /**
   * Converts the specified {@code String str} from this format to the specified {@code format}. A
   * "best effort" approach is taken; if {@code str} does not conform to the assumed format, then
   * the behavior of this method is undefined but we make a reasonable effort at converting anyway.
   */
  public final String to(CaseFormat format, String str) {
    checkNotNull(format);
    checkNotNull(str);
    return (format == this) ? str : convert(format, str);
  }

  /**
   * Enum values can override for performance reasons.
   */
  String convert(CaseFormat format, String str) {
    // deal with camel conversion
    StringBuilder out = null;
    int i = 0;
    int j = -1;
    while ((j = _word_boundary.indexIn(str, ++j)) != -1) {
      if (i == 0) {
        // include some extra space for separators
        out = new StringBuilder(str.length() + 4 * _word_separator.length());
        out.append(format.normalizeFirstWord(str.substring(i, j)));
      } else {
        out.append(format.normalizeWord(str.substring(i, j)));
      }
      out.append(format._word_separator);
      i = j + _word_separator.length();
    }
    return (i == 0)
        ? format.normalizeFirstWord(str)
        : out.append(format.normalizeWord(str.substring(i))).toString();
  }

  abstract String normalizeWord(String word);

  private String normalizeFirstWord(String word) {
    return (this == LOWER_CAMEL) ? toLowerCase(word) : normalizeWord(word);
  }

  private static String firstCharOnlyToUpper(String word) {
    return (word.isEmpty())
        ? word
        : String.valueOf(toUpperCase(word.charAt(0)))
            + toLowerCase(word.substring(1));
  }

/*
 * Extracted from Ascii.java
 * @author Craig Berry
 * @author Gregory Kick
 * @since 7.0
 */

  /**
   * Returns a copy of the input character sequence in which all {@linkplain #isUpperCase(char)
   * uppercase ASCII characters} have been converted to lowercase. All other characters are copied
   * without modification.
   *
   * @since 14.0
   */
  private static String toLowerCase(CharSequence chars) {
    int length = chars.length();
    StringBuilder builder = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      builder.append(toLowerCase(chars.charAt(i)));
    }
    return builder.toString();
  }

  /**
   * If the argument is an {@linkplain #isUpperCase(char) uppercase ASCII character} returns the
   * lowercase equivalent. Otherwise returns the argument.
   */
  private static char toLowerCase(char c) {
    return isUpperCase(c) ? (char) (c ^ 0x20) : c;
  }

  /**
   * If the argument is a {@linkplain #isLowerCase(char) lowercase ASCII character} returns the
   * uppercase equivalent. Otherwise returns the argument.
   */
  private static char toUpperCase(char c) {
    return isLowerCase(c) ? (char) (c & 0x5f) : c;
  }

  /**
   * Indicates whether {@code c} is one of the twenty-six lowercase ASCII alphabetic characters
   * between {@code 'a'} and {@code 'z'} inclusive. All others (including non-ASCII characters)
   * return {@code false}.
   */
  private static boolean isLowerCase(char c) {
    return (c >= 'a') && (c <= 'z');
  }

  /**
   * Indicates whether {@code c} is one of the twenty-six uppercase ASCII alphabetic characters
   * between {@code 'A'} and {@code 'Z'} inclusive. All others (including non-ASCII characters)
   * return {@code false}.
   */
  private static boolean isUpperCase(char c) {
    return (c >= 'A') && (c <= 'Z');
  }
}
