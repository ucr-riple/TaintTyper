package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.TypeIndex;
import edu.ucr.cs.riple.taint.ucrtainting.util.SymbolUtils;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

/** abstract base class for {@link SymbolLocation}. */
public abstract class AbstractSymbolLocation implements SymbolLocation {

  /** Location kind of the targeted symbol */
  public final LocationKind kind;
  /** Path of the file containing the symbol, if available. */
  public final Path path;
  /** Enclosing class of the symbol. */
  public final Symbol.ClassSymbol enclosingClass;
  /** Target symbol. */
  public final Symbol target;
  /** Set of type indexes */
  public Set<TypeIndex> typeIndexSet;

  public static final Set<TypeIndex> ON_TYPE = TypeIndex.topLevel();

  public AbstractSymbolLocation(LocationKind kind, Symbol target) {
    this.kind = kind;
    this.enclosingClass = target.enclClass();
    this.path = SymbolUtils.getPathFromSymbol(target);
    this.target = target;
    this.typeIndexSet = ON_TYPE;
  }

  @Override
  public void setTypeIndexSet(@Nullable Set<TypeIndex> typeIndexSet) {
    this.typeIndexSet = (typeIndexSet == null || typeIndexSet.isEmpty()) ? ON_TYPE : typeIndexSet;
  }

  @Override
  public String toString() {
    return "kind="
        + kind
        + ", enclosingClass="
        + enclosingClass
        + ", target="
        + target
        + ", typeIndexSet="
        + typeIndexSet
        + ", path="
        + path
        + '}';
  }

  @Override
  public LocationKind getKind() {
    return kind;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AbstractSymbolLocation)) {
      return false;
    }
    AbstractSymbolLocation that = (AbstractSymbolLocation) o;
    return getKind() == that.getKind()
        && Objects.equals(path, that.path)
        && Objects.equals(enclosingClass, that.enclosingClass)
        && Objects.equals(target, that.target)
        && Objects.equals(typeIndexSet, that.typeIndexSet);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getKind(), path, enclosingClass, target, typeIndexSet);
  }

  @Override
  public Symbol getTarget() {
    return target;
  }

  @Override
  public Path path() {
    return path;
  }

  @Override
  public Set<TypeIndex> getTypeIndexSet() {
    return typeIndexSet;
  }
}
