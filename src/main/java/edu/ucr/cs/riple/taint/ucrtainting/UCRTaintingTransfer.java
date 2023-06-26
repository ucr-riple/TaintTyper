package edu.ucr.cs.riple.taint.ucrtainting;

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
import org.checkerframework.javacutil.trees.TreeBuilder;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeKind;


public class UCRTaintingTransfer extends CFTransfer {
  private final TreeBuilder treeBuilder;
  private final UCRTaintingAnnotatedTypeFactory aTypeFactory;
  public UCRTaintingTransfer(CFAnalysis analysis) {
    super(analysis);
    aTypeFactory = (UCRTaintingAnnotatedTypeFactory) analysis.getTypeFactory();
    ProcessingEnvironment env = aTypeFactory.getChecker().getProcessingEnvironment();
    treeBuilder = new TreeBuilder(env);
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(n, in);

    if(n.getType().getKind() == TypeKind.BOOLEAN) {
      for(Node arg: n.getArguments()) {
        AnnotatedTypeMirror argType = aTypeFactory.getAnnotatedType(arg.getTree());
        if(argType.hasAnnotation(aTypeFactory.RTAINTED)) {
          JavaExpression je = JavaExpression.fromNode(arg);
          CFStore thenStore = result.getThenStore();
          CFStore elseStore = result.getElseStore();
          thenStore.insertOrRefine(je, aTypeFactory.RUNTAINTED);
          elseStore.insertOrRefine(je, aTypeFactory.RUNTAINTED);
//          thenStore.replaceValue(je, new CFValue(analysis, AnnotationMirrorSet.singleton(aTypeFactory.RUNTAINTED), argType.getUnderlyingType()));
//          elseStore.replaceValue(je, new CFValue(analysis, AnnotationMirrorSet.singleton(aTypeFactory.RUNTAINTED), argType.getUnderlyingType()));
        }
      }
    }

    return result;
  }
}
