package com.imminentmeals.prestige.codegen;

import com.google.common.base.CaseFormat;
import javax.lang.model.element.Element;

/**
 * <p>Container for Presentation Controller binding data. Relates an @Controller to an @Presentation's
 * implementation and provides a name to use to refer to an instance of the @Controller's implementation.</p>
 * @author Dandre Allison
 */
/* package */ class PresentationControllerBinding {

  /* package */final Element controller;
  /* package */final String variable_name;
  /* package */final Element presentation_implementation;

  /**
   * <p>Constructs a {@link PresentationControllerBinding}.</p>
   * @param controller The @Controller
   * @param presentation_implementation The implementation of the @Controller's @Presentation
   */
  public PresentationControllerBinding(Element controller, Element presentation_implementation) {
    final String class_name = controller.getSimpleName() + "";
    this.controller = controller;
    variable_name = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, class_name);
    this.presentation_implementation = presentation_implementation;
  }
}
