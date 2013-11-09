package com.imminentmeals.prestige.example.models;

import com.imminentmeals.prestige.annotations.Model;
import com.imminentmeals.prestige.example.SelfStorageFacility;
import javax.annotation.Nonnull;

@Model
public interface StorageModel {

  void selfStorageFacility(@Nonnull SelfStorageFacility facility);

  @Nonnull SelfStorageFacility selfStorageFacility();
}
