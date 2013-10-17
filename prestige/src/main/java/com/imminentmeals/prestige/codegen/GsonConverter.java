package com.imminentmeals.prestige.codegen;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * Converter which uses GSON to serialize instances of class T to disk.
 */
public class GsonConverter<T> {

    public <V extends T> GsonConverter(Gson gson, Class<V> type) {
        _gson = gson;
        _type = type;
    }

    @SuppressWarnings("unchecked")
    public <V extends T> V from(InputStream in) {
        final Reader reader = new InputStreamReader(in, _UTF8);
        return (V) _gson.fromJson(reader, _type);
    }

    public <V extends T> void toStream(V object, OutputStream out) throws IOException {
        final Writer writer = new OutputStreamWriter(out, _UTF8);
        _gson.toJson(object, writer);
        writer.close();
    }

    private final Gson _gson;
    private final Class<? extends T> _type;
    private static final Charset _UTF8 = Charset.forName("UTF-8");
}
