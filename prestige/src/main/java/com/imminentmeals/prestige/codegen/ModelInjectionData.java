package com.imminentmeals.prestige.codegen;

import javax.lang.model.element.Element;
import static com.imminentmeals.prestige.codegen.AnnotationProcessor.MODEL_INJECTOR_SUFFIX;

/* package */ class ModelInjectionData {
  /* package */final String package_name;
  /* package */final Element variable;
  /* package */final String variable_name;
  /* package */final String class_name;
  /* package */final boolean should_serialize;

  /**
   * <p>Constructs a {@link ModelInjectionData}.</p>
   * @param variable The variable in which to inject the Model
   */
  public ModelInjectionData(String package_name, Element variable, String element_class,
      boolean should_serialize) {
    this.package_name = package_name;
    this.variable = variable;
    variable_name = variable.getSimpleName() + "";
    class_name = element_class.substring(package_name.length() + 1) + MODEL_INJECTOR_SUFFIX;
this.should_serialize = should_serialize;
  }
}
