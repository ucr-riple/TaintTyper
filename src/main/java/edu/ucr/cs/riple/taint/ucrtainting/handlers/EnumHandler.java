package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.util.SymbolUtils;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/** Handler for enums. This handler will make enums constant untainted. */
public class EnumHandler extends AbstractHandler {

  public EnumHandler(UCRTaintingAnnotatedTypeFactory typeFactory) {
    super(typeFactory);
  }

  @Override
  public void addAnnotationsFromDefaultForType(Element element, AnnotatedTypeMirror type) {
    if (SymbolUtils.isEnumConstant(element)) {
      typeFactory.makeUntainted(type);
    }
  }
}
