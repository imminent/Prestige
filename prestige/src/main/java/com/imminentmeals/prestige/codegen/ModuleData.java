package com.imminentmeals.prestige.codegen;

import java.util.List;

/* package */ class ModuleData {
  /* package */final String qualified_name;
  /* package */final String scope;
  /* package */final String class_name;
  /* package */final String package_name;
  /* package */final List<?> components;

  /**
   * <p>Constructs a {@link ModuleData}.</p>
   * @param scope The implementation scope the module provides
   */
  public ModuleData(String name, String scope, String class_name, String package_name,
      List<?> components) {
    qualified_name = name;
    this.scope = scope;
    this.class_name = class_name;
    this.package_name = package_name;
    this.components = components;
  }
}
