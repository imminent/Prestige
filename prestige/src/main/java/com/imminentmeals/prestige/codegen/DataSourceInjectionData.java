package com.imminentmeals.prestige.codegen;

import javax.lang.model.element.Element;
import static com.imminentmeals.prestige.codegen.AnnotationProcessor.DATA_SOURCE_INJECTOR_SUFFIX;

/* package */ class DataSourceInjectionData {
  /* package */final String package_name;
  /* package */final Element target;
  /* package */final String variable_name;
  /* package */final String class_name;

  /**
   * <p>Constructs a {@link DataSourceInjectionData}.</p>
   * @param target The target of the injection
   * @param variable The variable from the target in which to inject the Data Source
   */
  public DataSourceInjectionData(String package_name, Element target, Element variable,
      String element_class) {
    this.package_name = package_name;
    this.target = target;
    variable_name = variable.getSimpleName() + "";
    class_name = element_class.substring(package_name.length() + 1) + DATA_SOURCE_INJECTOR_SUFFIX;
  }
}
