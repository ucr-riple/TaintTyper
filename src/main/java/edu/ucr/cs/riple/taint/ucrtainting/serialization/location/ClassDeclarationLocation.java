package edu.ucr.cs.riple.taint.ucrtainting.serialization.location;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.LocationVisitor;

public class ClassDeclarationLocation extends AbstractSymbolLocation{


    public ClassDeclarationLocation(LocationKind kind, Symbol target, JCTree tree) {
        super(kind, target, tree);
    }

    @Override
    public <R, P> R accept(LocationVisitor<R, P> v, P p) {
        return null;
    }
}
