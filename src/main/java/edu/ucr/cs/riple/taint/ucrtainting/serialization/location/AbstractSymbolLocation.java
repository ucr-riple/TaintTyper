package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Serializer;
import java.net.URI;
import java.nio.file.Path;
import javax.lang.model.element.ElementKind;
import org.checkerframework.com.google.common.base.Preconditions;

/** abstract base class for {@link SymbolLocation}. */
public abstract class AbstractSymbolLocation implements SymbolLocation {

  /** Element kind of the targeted symbol */
  protected final ElementKind type;
  /** Path of the file containing the symbol, if available. */
  protected final Path path;
  /** Enclosing class of the symbol. */
  protected final Symbol.ClassSymbol enclosingClass;

  public AbstractSymbolLocation(ElementKind type, Symbol target) {
    Preconditions.checkArgument(
        type.equals(target.getKind()),
        "Cannot instantiate element of type: "
            + target.getKind()
            + " with location type of: "
            + type
            + ".");
    this.type = type;
    this.enclosingClass = target.enclClass();
    URI pathInURI =
        enclosingClass.sourcefile != null
            ? enclosingClass.sourcefile.toUri()
            : (enclosingClass.classfile != null ? enclosingClass.classfile.toUri() : null);
    this.path = Serializer.pathToSourceFileFromURI(pathInURI);
  }
}
