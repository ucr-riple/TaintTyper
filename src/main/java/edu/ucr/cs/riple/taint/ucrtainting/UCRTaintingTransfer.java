package edu.ucr.cs.riple.taint.ucrtainting;

import edu.ucr.cs.riple.taint.ucrtainting.serialization.Utility;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
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
          makeRPossiblyValidated(result, arg, methodInvocationNode);
        }

        Node receiver = methodInvocationNode.getTarget().getReceiver();
        if (receiver != null
            && !(receiver instanceof ImplicitThisNode)
            && receiver.getTree() != null) {
          makeRPossiblyValidated(result, receiver, methodInvocationNode);
        }
      }
    }

    if (aTypeFactory.enableLibraryCheck && aTypeFactory.enableSideEffect) {
      if (aTypeFactory.hasReceiver(methodInvocationNode.getTree())
          && methodInvocationNode.getArguments().size() > 0) {
        Node receiver = methodInvocationNode.getTarget().getReceiver();
        if (receiver != null
            && !(receiver instanceof ImplicitThisNode)
            && receiver.getTree() != null) {
          if (receiver instanceof LocalVariableNode || receiver instanceof FieldAccessNode) {
            // if the code is part of provided annotated packages or is present
            // in the stub files, then we don't need any custom handling for it.
            if (aTypeFactory.isInThirdPartyCode(methodInvocationNode.getTree())
                && !aTypeFactory.isPresentInStub(methodInvocationNode.getTree())) {
              if (!aTypeFactory.hasTaintedReceiver(methodInvocationNode.getTree())
                  && aTypeFactory.hasTaintedArgument(methodInvocationNode.getTree())) {
                makeTaintedBySideEffect(result, receiver);
              }
            }
          }
        }
      }
    }

    return result;
  }

  private void makeRPossiblyValidated(
      TransferResult<CFValue, CFStore> result, Node n, Node calledMethod) {
    AnnotatedTypeMirror type = aTypeFactory.getAnnotatedType(n.getTree());
    AnnotationMirror anno = type.getAnnotation();
    JavaExpression je = JavaExpression.fromNode(n);
    if (type.hasAnnotation(aTypeFactory.rTainted)) {
      CFStore thenStore = result.getThenStore();
      CFStore elseStore = result.getElseStore();
      thenStore.insertOrRefine(
          je,
          aTypeFactory.rPossiblyValidatedAM(Collections.singletonList(calledMethod.toString())));
      elseStore.insertOrRefine(
          je,
          aTypeFactory.rPossiblyValidatedAM(Collections.singletonList(calledMethod.toString())));
    } else if (anno != null && aTypeFactory.isAccumulatorAnnotation(anno)) {
      CFStore thenStore = result.getThenStore();
      CFStore elseStore = result.getElseStore();
      List<String> calledMethods = aTypeFactory.getAccumulatedValues(anno);
      calledMethods.add(calledMethod.toString());
      thenStore.insertOrRefine(je, aTypeFactory.rPossiblyValidatedAM(calledMethods));
      elseStore.insertOrRefine(je, aTypeFactory.rPossiblyValidatedAM(calledMethods));
    }
  }

  private void makeTaintedBySideEffect(TransferResult<CFValue, CFStore> result, Node n) {
    AnnotatedTypeMirror type = aTypeFactory.getAnnotatedType(n.getTree());
    if (type.hasAnnotation(aTypeFactory.rUntainted)) {
      JavaExpression je = JavaExpression.fromNode(n);
      CFStore thenStore = result.getThenStore();
      CFStore elseStore = result.getElseStore();
      CFValue thenVal = thenStore.getValue(je);
      CFValue elseVal = elseStore.getValue(je);
      if (thenVal != null) {
        thenVal =
            new CFValue(
                analysis,
                AnnotationMirrorSet.singleton(aTypeFactory.rTainted),
                thenVal.getUnderlyingType());
        thenStore.replaceValue(je, thenVal);
      }
      if (elseVal != null) {
        elseVal =
            new CFValue(
                analysis,
                AnnotationMirrorSet.singleton(aTypeFactory.rTainted),
                elseVal.getUnderlyingType());
        elseStore.replaceValue(je, elseVal);
      }
    }
  }
}
