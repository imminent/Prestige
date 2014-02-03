package com.imminentmeals.prestige.codegen;

import javax.lang.model.element.Element;

/**
 * <p>Container for Presentation data.</p>
 *
 * @author Dandre Allison
 */
/* package */class PresentationData {
  /** The Protocol */
  protected final Element protocol;
  /** The Presentation implementation */
  protected final Element implementation;

  /**
   * <p>Constructs a {@link PresentationData}.</p>
   *
   * @param protocol The Protocol
   * @param implementation The presentation implementation
   */
  public PresentationData(Element protocol, Element implementation) {
    this.protocol = protocol;
    this.implementation = implementation;
  }

  @Override public String toString() {
    return String.format(format, protocol, implementation);
  }

  private static final String format = "{protocol: %s, implementation: %s}";
}
