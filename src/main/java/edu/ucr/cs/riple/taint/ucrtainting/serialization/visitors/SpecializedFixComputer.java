package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.SymbolLocation;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public abstract class SpecializedFixComputer extends SimpleTreeVisitor<Set<Fix>, FoundRequired> {

  /**
   * The type factory of the checker. Used to get the type of the tree and generate a fix only if is
   * {@link edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted}.
   */
  protected final UCRTaintingAnnotatedTypeFactory typeFactory;

  protected final FixComputer fixComputer;

  protected final TypeMatchVisitor typeMatchVisitor;
  protected final Context context;

  public SpecializedFixComputer(
      UCRTaintingAnnotatedTypeFactory typeFactory, FixComputer fixComputer, Context context) {
    this.context = context;
    this.typeFactory = typeFactory;
    this.fixComputer = fixComputer;
    this.typeMatchVisitor = new TypeMatchVisitor(typeFactory);
  }

  /**
   * Builds the fix for the given element.
   *
   * @param element The element to build the fix for.
   * @return The fix for the given element.
   */
  @Nullable
  public Fix buildFixForElement(Element element, FoundRequired pair) {
    SymbolLocation location = buildLocationForElement(element);
    if (location == null) {
      return null;
    }
    List<List<Integer>> indices = typeMatchVisitor.visit(pair.found, pair.required, null);
    AnnotatedTypeMirror elementAnnotatedType = typeFactory.getAnnotatedType(element);
    // remove redundant indices.
    indices =
        indices.stream()
            .filter(
                integers ->
                    !typeFactory.hasUntaintedAnnotation(
                        Utility.getAnnotatedTypeMirrorOfTypeArgumentAt(
                            elementAnnotatedType, integers)))
            .collect(Collectors.toList());
    location.setTypeVariablePositions(indices);
    if (indices.isEmpty()) {
      return null;
    }
    return new Fix(location);
  }

  /**
   * Builds the location for the given element.
   *
   * @param element The element to build the location for.
   * @return The location for the given element.
   */
  protected SymbolLocation buildLocationForElement(Element element) {
    if (element == null) {
      return null;
    }
    return SymbolLocation.createLocationFromSymbol((Symbol) element, context);
  }

  /**
   * Returns the effective type of the given type. If the given type is a method, returns the return
   * type.
   *
   * @param type The given type.
   * @return The effective type of the given type.
   */
  protected AnnotatedTypeMirror effectiveAnnotatedTypeMirror(AnnotatedTypeMirror type) {
    if (type instanceof AnnotatedTypeMirror.AnnotatedExecutableType) {
      return ((AnnotatedTypeMirror.AnnotatedExecutableType) type).getReturnType();
    }
    return type;
  }
}
