package edu.ucr.cs.riple.taint.ucrtainting;

import javax.lang.model.type.TypeKind;
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

    return result;
  }

  private void updateStore(TransferResult<CFValue, CFStore> result, Node receiver) {
    AnnotatedTypeMirror receiverType = aTypeFactory.getAnnotatedType(receiver.getTree());
    if (receiverType.hasAnnotation(aTypeFactory.RTAINTED)) {
      JavaExpression je = JavaExpression.fromNode(receiver);
      CFStore thenStore = result.getThenStore();
      CFStore elseStore = result.getElseStore();
      thenStore.insertOrRefine(je, aTypeFactory.RUNTAINTED);
      elseStore.insertOrRefine(je, aTypeFactory.RUNTAINTED);
    }
  }
}
