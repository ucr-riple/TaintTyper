package edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Context;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class PolyMethodVisitor extends SpecializedFixComputer {

  private final boolean activation;

  public PolyMethodVisitor(
      UCRTaintingAnnotatedTypeFactory typeFactory, FixComputer fixComputer, Context context) {
    super(typeFactory, fixComputer, context);
    this.activation = typeFactory.polyTaintInferenceEnabled();
  }

  @Override
  public Set<Fix> visitMethodInvocation(MethodInvocationTree node, FoundRequired pair) {
    if (!activation) {
      return Set.of();
    }
    Symbol.MethodSymbol calledMethod = (Symbol.MethodSymbol) TreeUtils.elementFromUse(node);
    AnnotatedTypeMirror returnTypeCopy =
        typeFactory.getAnnotatedType(calledMethod).getReturnType().deepCopy(true);
    typeFactory.replacePolyWithUntainted(returnTypeCopy);
    AnnotatedTypeMirror widenedValueType =
        typeFactory.getWidenedType(returnTypeCopy, pair.required);
    if (typeFactory.getTypeHierarchy().isSubtype(widenedValueType, pair.required)) {
      // we should add a fix for each poly tainted argument
      Set<Fix> onArguments = new HashSet<>();
      for (int i = 0; i < node.getArguments().size(); i++) {
        AnnotatedTypeMirror argType = typeFactory.getAnnotatedType(node.getArguments().get(i));
        AnnotatedTypeMirror formalParameterAnnotatedTypeMirror =
            typeFactory.getAnnotatedType(calledMethod.getParameters().get(i));
        // Check if the formal parameter is poly tainted.
        if (!typeFactory.hasPolyTaintedAnnotation(formalParameterAnnotatedTypeMirror)) {
          continue;
        }
        AnnotatedTypeMirror copyArg = argType.deepCopy(true);
        typeFactory.replacePolyWithUntainted(copyArg, formalParameterAnnotatedTypeMirror);
        onArguments.addAll(
            node.getArguments()
                .get(i)
                .accept(fixComputer, FoundRequired.of(argType, copyArg, pair.depth)));
      }
      return onArguments;
    }
    return Set.of();
  }
}
