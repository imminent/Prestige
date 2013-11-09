package com.imminentmeals.prestige.example;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class SelfStorageFacility {

  /* package */SelfStorageFacility(Context context) {
    _context = context;
  }

  @Nonnull public InputStream getFile(String name) throws FileNotFoundException {
    return _context.openFileInput(name);
  }

  @Nonnull public OutputStream openFile(String name) {
    try {
      return _context.openFileOutput(name, Context.MODE_PRIVATE);
    } catch (FileNotFoundException exception) {
      return new ByteArrayOutputStream();
    }
  }

  private final Context _context;
}
