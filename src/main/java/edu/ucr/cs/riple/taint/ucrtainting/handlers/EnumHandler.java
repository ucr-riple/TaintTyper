package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class EnumHandler extends AbstractHandler {

  public EnumHandler(UCRTaintingAnnotatedTypeFactory typeFactory) {
    super(typeFactory);
  }

  @Override
  public void addAnnotationsFromDefaultForType(Element element, AnnotatedTypeMirror type) {
    if (Utility.isEnumConstant(element)) {
      typeFactory.makeUntainted(type);
    }
  }
}
