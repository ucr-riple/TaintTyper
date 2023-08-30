package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.google.common.collect.ImmutableList;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Serializer;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;

/** abstract base class for {@link SymbolLocation}. */
public abstract class AbstractSymbolLocation implements SymbolLocation {

  /** Element kind of the targeted symbol */
  public final ElementKind kind;
  /** Path of the file containing the symbol, if available. */
  public final Path path;
  /** Enclosing class of the symbol. */
  public final Symbol.ClassSymbol enclosingClass;
  /** Declaration tree of the symbol. */
  public final JCTree declarationTree;
  /** Target symbol. */
  public final Symbol target;
  /** List of indexes to locate the type variable. */
  public List<List<Integer>> typeVariablePositions;

  public static final ImmutableList<List<Integer>> ON_TYPE = ImmutableList.of(List.of(0));

  public AbstractSymbolLocation(ElementKind kind, Symbol target, JCTree tree) {
    this.kind = kind;
    this.enclosingClass = target.enclClass();
    URI pathInURI =
        enclosingClass.sourcefile != null
            ? enclosingClass.sourcefile.toUri()
            : (enclosingClass.classfile != null ? enclosingClass.classfile.toUri() : null);
    this.path = Serializer.pathToSourceFileFromURI(pathInURI);
    this.declarationTree = tree;
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
        + ", path="
        + path
        + ", enclosingClass="
        + enclosingClass
        + ", target="
        + target
        + ", typeVariablePositions="
        + typeVariablePositions
        + '}';
  }

  @Override
  public ElementKind getKind() {
    return kind;
  }
}
