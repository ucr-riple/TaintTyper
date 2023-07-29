package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.Element;

public class StaticFinalFieldHandler extends AbstractHandler {

  private final Set<Element> staticFinalFields;

  public StaticFinalFieldHandler() {
    this.staticFinalFields = new HashSet<>();
  }
}
