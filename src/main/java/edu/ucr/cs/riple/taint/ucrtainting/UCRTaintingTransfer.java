package edu.ucr.cs.riple.taint.ucrtainting;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeKind;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
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
    ProcessingEnvironment env = aTypeFactory.getChecker().getProcessingEnvironment();
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(n, in);

    // validation
    if (aTypeFactory.ENABLE_CUSTOM_CHECK) {
      if (n.getType().getKind() == TypeKind.BOOLEAN) {
        for (Node arg : n.getArguments()) {
          AnnotatedTypeMirror argType = aTypeFactory.getAnnotatedType(arg.getTree());
          if (argType.hasAnnotation(aTypeFactory.RTAINTED)) {
            JavaExpression je = JavaExpression.fromNode(arg);
            CFStore thenStore = result.getThenStore();
            CFStore elseStore = result.getElseStore();
            thenStore.insertOrRefine(je, aTypeFactory.RUNTAINTED);
            elseStore.insertOrRefine(je, aTypeFactory.RUNTAINTED);
          }
        }

        AnnotatedTypeMirror receiverType = aTypeFactory.getAnnotatedType(n.getTarget().getTree());
        if (receiverType.hasAnnotation(aTypeFactory.RTAINTED)) {
          JavaExpression je = JavaExpression.fromNode(n.getTarget());
          CFStore thenStore = result.getThenStore();
          CFStore elseStore = result.getElseStore();
          thenStore.insertOrRefine(je, aTypeFactory.RUNTAINTED);
          elseStore.insertOrRefine(je, aTypeFactory.RUNTAINTED);
        }
      }
    }

    return result;
  }
}
