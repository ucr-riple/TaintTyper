package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.google.common.collect.ImmutableList;
import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Serializer;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
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
  /** List of indexes to locate the type variable. */
  public List<List<Integer>> typeVariablePositions;

  public static final ImmutableList<List<Integer>> ON_TYPE = ImmutableList.of(List.of(0));

  public AbstractSymbolLocation(LocationKind kind, Symbol target) {
    this.kind = kind;
    this.enclosingClass = target.enclClass();
    URI pathInURI =
        enclosingClass.sourcefile != null
            ? enclosingClass.sourcefile.toUri()
            : (enclosingClass.classfile != null ? enclosingClass.classfile.toUri() : null);
    this.path = Serializer.pathToSourceFileFromURI(pathInURI);
    this.target = target;
    this.typeVariablePositions = ON_TYPE;
  }

  public void setTypeVariablePositions(@Nullable List<List<Integer>> typeVariablePositions) {
    this.typeVariablePositions =
        (typeVariablePositions == null || typeVariablePositions.isEmpty())
            ? ON_TYPE
            : typeVariablePositions;
  }

  @Override
  public String toString() {
    return "kind="
        + kind
        + ", enclosingClass="
        + enclosingClass
        + ", target="
        + target
        + ", typeVariablePositions="
        + typeVariablePositions
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
        && Objects.equals(typeVariablePositions, that.typeVariablePositions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getKind(), path, enclosingClass, target, typeVariablePositions);
  }

  @Override
  public Symbol getTarget() {
    return target;
  }
}
