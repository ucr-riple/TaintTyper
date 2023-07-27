package edu.ucr.cs.riple.taint.ucrtainting;

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
      MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(n, in);

    // validation
    if (aTypeFactory.ENABLE_VALIDATION_CHECK) {
      if (n.getType().getKind() == TypeKind.BOOLEAN) {
        for (Node arg : n.getArguments()) {
          updateStore(result, arg, n);
        }

        Node receiver = n.getTarget().getReceiver();
        if (receiver != null
            && !(receiver instanceof ImplicitThisNode)
            && receiver.getTree() != null) {
          updateStore(result, receiver, n);
        }
      }
    }

    if (aTypeFactory.ENABLE_LIBRARY_CHECK) {
      if (aTypeFactory.returnsThis(n.getTree())) {
        System.out.println("hi");
      }
      if (aTypeFactory.hasReceiver(n.getTree()) && n.getArguments().size() > 0) {
        Node receiver = n.getTarget().getReceiver();
        if (receiver != null
            && !(receiver instanceof ImplicitThisNode)
            && receiver.getTree() != null) {
          if (receiver instanceof LocalVariableNode || receiver instanceof FieldAccessNode) {
            // if the code is part of provided annotated packages or is present
            // in the stub files, then we don't need any custom handling for it.
            if (!aTypeFactory.hasAnnotatedPackage(n.getTree())
                && !aTypeFactory.isPresentInStub(n.getTree())) {
              if (!aTypeFactory.hasTaintedReceiver(n.getTree())
                  && aTypeFactory.hasTaintedArgument(n.getTree())) {
                updateStoreTaint(result, receiver);
              }
            }
          }
        }
      }
    }

    return result;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitObjectCreation(
      ObjectCreationNode n, TransferInput<CFValue, CFStore> p) {
    return super.visitObjectCreation(n, p);
  }

  @Override
  public TransferResult<CFValue, CFStore> visitAssignment(
      AssignmentNode n, TransferInput<CFValue, CFStore> in) {
    return super.visitAssignment(n, in);
  }

  private void updateStore(TransferResult<CFValue, CFStore> result, Node n, Node calledMethod) {
    AnnotatedTypeMirror type = aTypeFactory.getAnnotatedType(n.getTree());
    AnnotationMirror anno = type.getAnnotation();
    JavaExpression je = JavaExpression.fromNode(n);
    if (type.hasAnnotation(aTypeFactory.RTAINTED)) {
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

  private void updateStoreTaint(TransferResult<CFValue, CFStore> result, Node n) {
    AnnotatedTypeMirror type = aTypeFactory.getAnnotatedType(n.getTree());
    if (type.hasAnnotation(aTypeFactory.RUNTAINTED)) {
      JavaExpression je = JavaExpression.fromNode(n);
      CFStore thenStore = result.getThenStore();
      CFStore elseStore = result.getElseStore();
      CFValue thenVal = thenStore.getValue(je);
      CFValue elseVal = elseStore.getValue(je);
      if (thenVal != null) {
        thenVal =
            new CFValue(
                analysis,
                new AnnotationMirrorSet(aTypeFactory.RTAINTED),
                thenVal.getUnderlyingType());
        thenStore.replaceValue(je, thenVal);
      }
      if (elseVal != null) {
        elseVal =
            new CFValue(
                analysis,
                new AnnotationMirrorSet(aTypeFactory.RTAINTED),
                elseVal.getUnderlyingType());
        elseStore.replaceValue(je, elseVal);
      }
    }
  }
}
