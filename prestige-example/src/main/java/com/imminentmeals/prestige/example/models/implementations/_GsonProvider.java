package com.imminentmeals.prestige.example.models.implementations;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.imminentmeals.prestige.GsonProvider;
import com.imminentmeals.prestige.annotations.InjectModel;
import com.imminentmeals.prestige.annotations.ModelImplementation;
import com.imminentmeals.prestige.example.models.StorageModel;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.Nonnull;

@ModelImplementation(serialize = false)
/* package */class _GsonProvider implements GsonProvider {

  @InjectModel
  /* package */_GsonProvider(StorageModel storage) {
    _gson_builder = new GsonBuilder();
    _gson = _gson_builder.create();
    _storage = storage;
  }

  @Override
  @Nonnull public GsonBuilder gsonBuilder() {
    return _gson_builder;
  }

  @Override
  @Nonnull public Gson gson() {
    return _gson;
  }

  @Override
  @Nonnull public InputStream inputStreamFor(Class type) {
    try {
      return _storage.selfStorageFacility().getFile(type.getName() + _FILE_FORMAT);
    } catch (FileNotFoundException error) {
      throw new RuntimeException(error);
    }
  }

  @Override
  @Nonnull public OutputStream outputStreamFor(Class type) {
    return _storage.selfStorageFacility().openFile(type.getName() + _FILE_FORMAT);
  }

  private final GsonBuilder _gson_builder;
  private final Gson _gson;
  private StorageModel _storage;
  private static final String _FILE_FORMAT = ".json";
}
