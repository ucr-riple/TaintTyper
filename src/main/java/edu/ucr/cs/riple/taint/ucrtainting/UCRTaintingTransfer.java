package edu.ucr.cs.riple.taint.ucrtainting;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import edu.ucr.cs.riple.taint.ucrtainting.qual.RThis;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import java.util.Collections;
import java.util.List;
import javax.lang.model.type.TypeKind;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.*;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationMirrorSet;

public class UCRTaintingTransfer extends CFTransfer {
  private final UCRTaintingAnnotatedTypeFactory aTypeFactory;

  public UCRTaintingTransfer(CFAnalysis analysis) {
    super(analysis);
    aTypeFactory = (UCRTaintingAnnotatedTypeFactory) analysis.getTypeFactory();
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      MethodInvocationNode methodInvocationNode, TransferInput<CFValue, CFStore> in) {

    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(methodInvocationNode, in);

    // Assume any receiver or argument involved in
    // a boolean method invocation to be validated
    if (aTypeFactory.enableValidationCheck) {
      if (Utility.isMethodInvocationInIfConditional(methodInvocationNode.getTreePath())
          && methodInvocationNode.getType().getKind() == TypeKind.BOOLEAN) {
        for (Node arg : methodInvocationNode.getArguments()) {
          makePossiblyValidated(result, arg, methodInvocationNode);
        }

        Node receiver = methodInvocationNode.getTarget().getReceiver();
        if (receiver != null
            && !(receiver instanceof ImplicitThisNode)
            && receiver.getTree() != null) {
          makePossiblyValidated(result, receiver, methodInvocationNode);
        }
      }
    }

    // If any argument is tainted, make the receiver tainted.
    // Only perform if enableSideEffect flag is on along with enableLibraryCheck flag.
    if (aTypeFactory.enableLibraryCheck && aTypeFactory.enableSideEffect) {
      if (isSideEffectCandidate(methodInvocationNode)) {
        // if the code is part of provided annotated packages or is present
        // in the stub files, then we don't need any custom handling for it.
        MethodInvocationTree tree = methodInvocationNode.getTree();
        handleSideEffect(
            tree,
            result,
            methodInvocationNode,
            aTypeFactory.hasTaintedArgument(tree) && !aTypeFactory.hasTaintedReceiver(tree));
      }
    }

    return result;
  }

  private void handleSideEffect(
      ExpressionTree tree,
      TransferResult<CFValue, CFStore> result,
      MethodInvocationNode node,
      boolean isTainted) {
    AnnotatedTypeMirror rAnno = aTypeFactory.getAnnotatedType(node.getTree());
    if (aTypeFactory.isUnannotatedThirdParty(tree) || rAnno.hasAnnotation(RThis.class)) {
      if (node.getTarget().getReceiver() instanceof MethodInvocationNode) {
        handleSideEffect(
            tree,
            result,
            (MethodInvocationNode) node.getTarget().getReceiver(),
            (aTypeFactory.hasTaintedArgument(tree) && !aTypeFactory.hasTaintedReceiver(tree))
                || isTainted);
      } else {
        if (isTainted) {
          makeStoresTainted(result, node.getTarget().getReceiver());
        }
      }
    }
  }

  private boolean isSideEffectCandidate(MethodInvocationNode methodInvocationNode) {
    if (methodInvocationNode.getArguments().size() > 0) {
      Node receiver = methodInvocationNode.getTarget().getReceiver();
      if (receiver != null && !(receiver instanceof ImplicitThisNode)) {
        if (receiver instanceof LocalVariableNode
            || receiver instanceof FieldAccessNode
            || receiver instanceof MethodInvocationNode) {
          return true;
        }
      }
    }
    return false;
  }

  private void makePossiblyValidated(
      TransferResult<CFValue, CFStore> result, Node node, Node calledMethod) {
    AnnotatedTypeMirror type = aTypeFactory.getAnnotatedType(node.getTree());
    JavaExpression je = JavaExpression.fromNode(node);
    List<String> calledMethods;

    if (type.hasAnnotation(aTypeFactory.rTainted)) {
      calledMethods = Collections.singletonList(calledMethod.toString());
      insertOrRefineRPossiblyValidated(result, je, calledMethods);
    } else if (type.getAnnotation() != null
        && aTypeFactory.isAccumulatorAnnotation(type.getAnnotation())) {
      calledMethods = aTypeFactory.getAccumulatedValues(type.getAnnotation());
      calledMethods.add(calledMethod.toString());
      insertOrRefineRPossiblyValidated(result, je, calledMethods);
    }
  }

  private void insertOrRefineRPossiblyValidated(
      TransferResult<CFValue, CFStore> result, JavaExpression je, List<String> calledMethods) {
    result.getThenStore().insertOrRefine(je, aTypeFactory.rPossiblyValidatedAM(calledMethods));
    result.getElseStore().insertOrRefine(je, aTypeFactory.rPossiblyValidatedAM(calledMethods));
  }

  private void makeStoresTainted(TransferResult<CFValue, CFStore> result, Node n) {
    if (n.getTree() == null) {
      return;
    }
    AnnotatedTypeMirror type = aTypeFactory.getAnnotatedType(n.getTree());
    if (type.hasAnnotation(aTypeFactory.rUntainted)) {
      JavaExpression je = JavaExpression.fromNode(n);

      CFValue thenVal = result.getThenStore().getValue(je);
      if (thenVal != null) {
        thenVal =
            new CFValue(
                analysis,
                AnnotationMirrorSet.singleton(aTypeFactory.rTainted),
                thenVal.getUnderlyingType());
        result.getThenStore().replaceValue(je, thenVal);
      }

      CFValue elseVal = result.getElseStore().getValue(je);
      if (elseVal != null) {
        elseVal =
            new CFValue(
                analysis,
                AnnotationMirrorSet.singleton(aTypeFactory.rTainted),
                elseVal.getUnderlyingType());
        result.getElseStore().replaceValue(je, elseVal);
      }
    }
  }
}
