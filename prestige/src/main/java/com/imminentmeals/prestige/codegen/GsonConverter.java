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

  public GsonConverter(Gson gson, Class<T> type) {
    _gson = gson;
    _type = type;
  }

  public T from(InputStream in) {
    final Reader reader = new InputStreamReader(in, _UTF_8);
    return _gson.fromJson(reader, _type);
  }

  public void toStream(Object object, OutputStream out) throws IOException {
    if (!_type.isInstance(object)) return;
    final Writer writer = new OutputStreamWriter(out, _UTF_8);
    _gson.toJson(object, writer);
    writer.close();
  }

  private final Gson _gson;
  private final Class<T> _type;
  private final Charset _UTF_8 = Charset.forName("UTF-8");
}
