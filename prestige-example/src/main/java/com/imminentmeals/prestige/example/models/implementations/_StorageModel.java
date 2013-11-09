package com.imminentmeals.prestige.example.models.implementations;

import com.imminentmeals.prestige.annotations.ModelImplementation;
import com.imminentmeals.prestige.example.SelfStorageFacility;
import com.imminentmeals.prestige.example.models.StorageModel;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@ModelImplementation(serialize = false)
public class _StorageModel implements StorageModel {

  @Override
  public void selfStorageFacility(SelfStorageFacility facility) {
    _facility = facility;
  }

  @Override
  @Nonnull public SelfStorageFacility selfStorageFacility() {
    if (_facility == null) {
      throw new IllegalStateException(
          "Set one with selfStorageFacility(SelfStorageFacility) first");
    }
    return _facility;
  }

  private SelfStorageFacility _facility;
}
