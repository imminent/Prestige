package com.imminentmeals.prestige.codegen;

import javax.lang.model.element.Element;

/**
 * <p>Container for Presentation Fragment data.</p>
 * @author Dandre Allison
 */
/* package */ class PresentationFragmentData extends PresentationData {

  /**
   * <p>Constructs a {@link com.imminentmeals.prestige.codegen.PresentationData}.</p>
   * @param protocol The Protocol
   * @param implementation The presentation implementation
   */
  public PresentationFragmentData(Element protocol, Element implementation) {
    super(protocol, implementation);
  }

  @Override
  public String toString() {
    return String.format(format, protocol, implementation);
  }

  private static final String format = "{protocol: %s, implementation: %s}";
}
