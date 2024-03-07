package edu.ucr.cs.riple.taint.ucrtainting;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.typeannotator.DefaultForTypeAnnotator;

public class UCRTaintingTypeAnnotator extends DefaultForTypeAnnotator {

  UCRTaintingAnnotatedTypeFactory factory;

  /**
   * Creates a new TypeAnnotator.
   *
   * @param atypeFactory the type factory
   */
  protected UCRTaintingTypeAnnotator(AnnotatedTypeFactory atypeFactory) {
    super(atypeFactory);
    this.factory = (UCRTaintingAnnotatedTypeFactory) atypeFactory;
  }

  @Override
  public Void visitDeclared(AnnotatedTypeMirror.AnnotatedDeclaredType type, Void unused) {
    return super.visitDeclared(type, unused);
  }

  @Override
  public Void visitArray(AnnotatedTypeMirror.AnnotatedArrayType type, Void unused) {
    factory.makeUntainted(type);
    return super.visitArray(type, unused);
  }
}
