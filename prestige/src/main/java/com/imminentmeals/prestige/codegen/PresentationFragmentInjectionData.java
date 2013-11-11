package com.imminentmeals.prestige.codegen;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Map;
import javax.lang.model.element.Element;

import static com.imminentmeals.prestige.codegen.AnnotationProcessor.PRESENTATION_FRAGMENT_INJECTOR_SUFFIX;

/* package */ class PresentationFragmentInjectionData {
  /* package */final String package_name;
  /* package */final Element variable;
  /* package */final String variable_name;
  /* package */final String class_name;
  /* package */final Map<Integer, Integer> displays;
  /* package */final String tag;
  /* package */final Element implementation;
  /* package */final boolean is_manually_created;

  public PresentationFragmentInjectionData(String package_name, Element variable
      , String element_class, int[] display_list, String tag, Element implementation
      , boolean is_manually_created) {
    // Defensive copies array before using its values
    display_list = Arrays.copyOf(display_list, display_list.length);
    assert display_list.length % 2 == 0;

    this.package_name = package_name;
    this.variable = variable;
    variable_name = variable.getSimpleName() + "";
    class_name = element_class.substring(package_name.length() + 1) + PRESENTATION_FRAGMENT_INJECTOR_SUFFIX;
    this.implementation = implementation;
    this.is_manually_created = is_manually_created;

    if (this.is_manually_created) {
      this.displays = null;
      this.tag = null;
      return;
    }

    final ImmutableMap.Builder<Integer, Integer> builder = ImmutableMap.builder();
    for (int i = 0; i < display_list.length; i += 2) builder.put(display_list[i], display_list[i + 1]);
    this.displays = builder.build();
    this.tag = tag;
  }
}
