package com.imminentmeals.prestige;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.imminentmeals.prestige.annotations.Model;
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.Nonnull;

/**
 * Provides a configured {@link com.google.gson.Gson} JSON parser.
 */
@Model @SuppressWarnings("UnusedDeclaration")
public interface GsonProvider {

  @Nonnull GsonBuilder gsonBuilder();

  /**
   * Provides a configured {@link com.google.gson.Gson} JSON parser.
   *
   * @return A JSON parser
   */
  @Nonnull Gson gson();

  @Nonnull InputStream inputStreamFor(Class type);

  @Nonnull OutputStream outputStreamFor(Class type);
}
