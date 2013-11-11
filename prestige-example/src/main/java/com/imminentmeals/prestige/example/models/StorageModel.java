package com.imminentmeals.prestige.example.models;

import com.imminentmeals.prestige.annotations.Model;
import com.imminentmeals.prestige.example.SelfStorageFacility;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@Model @ParametersAreNonnullByDefault
public interface StorageModel {

  void selfStorageFacility(SelfStorageFacility facility);

  @Nonnull SelfStorageFacility selfStorageFacility();
}
