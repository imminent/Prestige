package com.imminentmeals.prestige;

import com.google.gson.Gson;
import com.imminentmeals.prestige.annotations.Model;

import java.io.File;
import java.io.OutputStream;

import javax.annotation.Nonnull;

/**
 * Provides a configured {@link com.google.gson.Gson} JSON parser.
 */
@Model
public interface GsonProvider {

    /**
     * Provides a configured {@link com.google.gson.Gson} JSON parser.
     * @return A JSON parser
     */
    @Nonnull Gson gson();

    @Nonnull File fileFor(Class type);

    @Nonnull OutputStream outputStreamFor(Class type);
}
