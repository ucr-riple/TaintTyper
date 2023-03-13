package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import javax.lang.model.element.ElementKind;

/** subtype of {@link AbstractSymbolLocation} targeting local variables. */
public class LocalVariableLocation extends AbstractSymbolLocation {

  /** Position of the local variable in the source file */
  public final int pos;

  public LocalVariableLocation(Symbol target, JCTree declarationTree, Type type) {
    super(ElementKind.LOCAL_VARIABLE, target, declarationTree, type);
    this.pos = declarationTree != null ? declarationTree.getStartPosition() : -1;
  }
}
