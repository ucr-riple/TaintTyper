package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.location.SymbolLocation;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;

public abstract class SpecializedFixComputer extends SimpleTreeVisitor<Set<Fix>, FoundRequired> {

  /** The javac context. */
  protected final Context context;
  /**
   * The type factory of the checker. Used to get the type of the tree and generate a fix only if is
   * {@link edu.ucr.cs.riple.taint.ucrtainting.qual.RTainted}.
   */
  protected final UCRTaintingAnnotatedTypeFactory typeFactory;

  protected final FixComputer fixComputer;

  protected final TypeMatchVisitor typeMatchVisitor;

  public SpecializedFixComputer(
      Context context, UCRTaintingAnnotatedTypeFactory typeFactory, FixComputer fixComputer) {
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
    if (pair != null && pair.required != null && pair.found != null) {
      location.setTypeVariablePositions(typeMatchVisitor.visit(pair.found, pair.required, null));
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
}
