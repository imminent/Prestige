package com.imminentmeals.prestige.codegen;

import javax.lang.model.element.Element;
import static com.imminentmeals.prestige.codegen.AnnotationProcessor.PRESENTATION_INJECTOR_SUFFIX;

/* package */ class PresentationInjectionData {
    /* package */final String package_name;
    /* package */final Element variable;
    /* package */final String variable_name;
    /* package */final String class_name;

    public PresentationInjectionData(String package_name, Element variable, String element_class) {
        this.package_name = package_name;
        this.variable = variable;
        variable_name = variable.getSimpleName() + "";
        class_name = element_class.substring(package_name.length() + 1) + PRESENTATION_INJECTOR_SUFFIX;
    }
}
