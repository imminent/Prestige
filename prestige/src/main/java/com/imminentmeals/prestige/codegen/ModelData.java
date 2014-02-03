package com.imminentmeals.prestige.codegen;

import java.util.List;
import javax.annotation.Nonnull;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

import static com.imminentmeals.prestige.codegen.utilities.CaseFormat.LOWER_UNDERSCORE;
import static com.imminentmeals.prestige.codegen.utilities.CaseFormat.UPPER_CAMEL;

/**
 * <p>Container for @Model data, groups the Model interface with its implementation</p>.
 *
 * @author Dandre Allison
 */
/* package */ class ModelData {
  /** The @ModelImplementation stored for use in setting up code generation */
  /* package */final Element implementation;
  /* package */final Element contract;
  /* package */final List<? extends VariableElement> parameters;
  /* package */final String variable_name;
  /* package */final boolean should_serialize;

  /**
   * <p> Constructs a {@link ModelData}. <p>
   *
   * @param model The @Model
   * @param should_serialize Indicates if serialization logic should be implemented for the model
   */
  public ModelData(@Nonnull Element model, boolean should_serialize) {
    this(model, null, null, should_serialize);
  }

  /**
   * <p> Constructs a {@link ModelData}. <p>
   *
   * @param model The @Model
   * @param model_implementation The implementation of the @Model
   * @param parameters Parameters of the model implementation's constructor (list of models on which
   * it depends)
   * @param should_serialize Indicates if serialization logic should be implemented for the model
   */
  public ModelData(@Nonnull Element model, Element model_implementation
      , List<? extends VariableElement> parameters, boolean should_serialize) {
    implementation = model_implementation;
    contract = model;
    this.parameters = parameters;
    variable_name =
        UPPER_CAMEL.to(LOWER_UNDERSCORE, model.getSimpleName() + "");
    this.should_serialize = should_serialize;
  }

  @Override public int hashCode() {
    return contract.hashCode();
  }

  @Override public boolean equals(Object object) {
    if (!(object instanceof ModelData)) return false;
    final ModelData other = (ModelData) object;
    return contract.equals(other.contract)
        && (implementation == null || other.implementation == null
        || implementation.equals(other.implementation));
  }

  @Override public String toString() {
    return contract + "";
  }
}
