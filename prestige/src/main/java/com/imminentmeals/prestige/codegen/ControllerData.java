package com.imminentmeals.prestige.codegen;

import javax.lang.model.element.Element;

/**
 * <p>Container for @Controller data, groups the Controller interface with its implementation</p>.
 * @author Dandre Allison
 */
/* package */ class ControllerData {
  /** The @ControllerImplementation stored for use in setting up code generation */
  /* package */final Element implementation;
  /* package */final Element contract;

  /**
   * <p>Constructs a {@link ControllerData}.<p>
   * @param controller The @Controller
   * @param controller_implementation The implementation of the @Controller
   */
  public ControllerData(Element controller, Element controller_implementation) {
    implementation = controller_implementation;
    contract = controller;
  }
}
