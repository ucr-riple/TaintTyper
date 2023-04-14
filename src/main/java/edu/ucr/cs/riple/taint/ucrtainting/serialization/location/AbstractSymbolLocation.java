package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Serializer;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import javax.lang.model.element.ElementKind;
import org.checkerframework.com.google.common.base.Preconditions;
import org.json.JSONArray;
import org.json.JSONObject;

/** abstract base class for {@link SymbolLocation}. */
public abstract class AbstractSymbolLocation implements SymbolLocation {

  /** Element kind of the targeted symbol */
  protected final ElementKind kind;
  /** Path of the file containing the symbol, if available. */
  protected final Path path;
  /** Enclosing class of the symbol. */
  protected final Symbol.ClassSymbol enclosingClass;
  /** Declaration tree of the symbol. */
  protected final JCTree declarationTree;
  /** Target symbol. */
  protected final Symbol target;
  /** Target type of the symbol. */
  protected Type targetType;

  public AbstractSymbolLocation(ElementKind kind, Symbol target, JCTree tree, Type targetType) {
    Preconditions.checkArgument(
        kind.equals(target.getKind()),
        "Cannot instantiate element of kind: "
            + target.getKind()
            + " with location kind of: "
            + kind
            + ".");
    this.kind = kind;
    this.enclosingClass = target.enclClass();
    URI pathInURI =
        enclosingClass.sourcefile != null
            ? enclosingClass.sourcefile.toUri()
            : (enclosingClass.classfile != null ? enclosingClass.classfile.toUri() : null);
    this.path = Serializer.pathToSourceFileFromURI(pathInURI);
    this.declarationTree = tree;
    this.targetType = targetType;
    this.target = target;
  }

  /**
   * @return the type variables of the symbol.
   */
  protected List<Type> getTypeVariables() {
    return target.type.tsym.type.getTypeArguments();
  }

  @Override
  public JSONObject toJSON() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("path", path != null ? path.toString() : "null");
    jsonObject.put("kind", kind.name());
    jsonObject.put("class", Serializer.serializeSymbol(this.enclosingClass));
    jsonObject.put("pos", declarationTree != null ? declarationTree.getStartPosition() : -1);
    jsonObject.put(
        "type-variables",
        new JSONArray(getTypeVariables().stream().map(Objects::toString).toArray()));
    jsonObject.put("target-type", targetType != null ? targetType.toString() : "null");
    return jsonObject;
  }
}
