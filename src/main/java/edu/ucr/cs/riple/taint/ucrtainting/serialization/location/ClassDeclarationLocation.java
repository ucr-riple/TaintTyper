package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.LocationVisitor;

public class ClassDeclarationLocation extends AbstractSymbolLocation {

  public final Type.ClassType toChange;

  public ClassDeclarationLocation(Symbol target, Type.ClassType toChange) {
    super(LocationKind.CLASS_DECLARATION, target);
    this.toChange = toChange;
  }

  @Override
  public SymbolLocation instance(Symbol target, Context context) {
    return new ClassDeclarationLocation(target, toChange);
  }

  @Override
  public <R, P> R accept(LocationVisitor<R, P> v, P p) {
    return v.visitClassDeclaration(this, p);
  }
}
