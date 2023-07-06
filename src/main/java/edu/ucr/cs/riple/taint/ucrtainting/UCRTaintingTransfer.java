package edu.ucr.cs.riple.taint.ucrtainting;

import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ImplicitThisNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationMirrorSet;

import javax.lang.model.type.TypeKind;

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
          updateStore(result, arg);
        }

        Node receiver = n.getTarget().getReceiver();
        if (receiver != null
            && !(receiver instanceof ImplicitThisNode)
            && receiver.getTree() != null) {
          updateStore(result, receiver);
        }
      }
    }

    if (aTypeFactory.ENABLE_LIBRARY_CHECK) {
      if (aTypeFactory.hasReceiver(n.getTree()) && n.getArguments().size() > 0) {
        Node receiver = n.getTarget().getReceiver();
        if (receiver != null
            && !(receiver instanceof ImplicitThisNode)
            && receiver.getTree() != null) {
          // if the code is part of provided annotated packages or is present
          // in the stub files, then we don't need any custom handling for it.
          if (!aTypeFactory.hasAnnotatedPackage(n.getTree())
              && !aTypeFactory.isPresentInStub(n.getTree())) {
              if(!aTypeFactory.hasTaintedReceiver(n.getTree()) && aTypeFactory.hasTaintedArgument(n.getTree())) {
                updateStoreTaint(result, receiver);
              }
          }
        }
      }
    }

    return result;
  }

  private void updateStore(TransferResult<CFValue, CFStore> result, Node n) {
    AnnotatedTypeMirror type = aTypeFactory.getAnnotatedType(n.getTree());
    if (type.hasAnnotation(aTypeFactory.RTAINTED)) {
      JavaExpression je = JavaExpression.fromNode(n);
      CFStore thenStore = result.getThenStore();
      CFStore elseStore = result.getElseStore();
      thenStore.insertOrRefine(je, aTypeFactory.RUNTAINTED);
      elseStore.insertOrRefine(je, aTypeFactory.RUNTAINTED);
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
      thenVal = new CFValue(analysis, new AnnotationMirrorSet(aTypeFactory.RTAINTED), thenVal.getUnderlyingType());
      elseVal = new CFValue(analysis, new AnnotationMirrorSet(aTypeFactory.RTAINTED), elseVal.getUnderlyingType());

      thenStore.replaceValue(je, thenVal);
      elseStore.replaceValue(je, elseVal);
    }
  }
}
