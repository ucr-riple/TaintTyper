/*
 * MIT License
 *
 * Copyright (c) 2024 University of California, Riverside
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.xxx.cs.yyyyy.taint.tainttyper;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import edu.xxx.cs.yyyyy.taint.tainttyper.qual.RThis;
import edu.xxx.cs.yyyyy.taint.tainttyper.util.SymbolUtils;
import java.util.Collections;
import java.util.List;
import javax.lang.model.type.TypeKind;
import org.checkerframework.common.accumulation.AccumulationStore;
import org.checkerframework.common.accumulation.AccumulationTransfer;
import org.checkerframework.common.accumulation.AccumulationValue;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.*;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.TreeUtils;

public class TaintTyperTransfer extends AccumulationTransfer {
  private final TaintTyperAnnotatedTypeFactory aTypeFactory;

  public TaintTyperTransfer(TaintTyperAnalysis analysis) {
    super(analysis);
    aTypeFactory = (TaintTyperAnnotatedTypeFactory) analysis.getTypeFactory();
  }

  @Override
  public TransferResult<AccumulationValue, AccumulationStore> visitMethodInvocation(
      MethodInvocationNode methodInvocationNode,
      TransferInput<AccumulationValue, AccumulationStore> in) {

    TransferResult<AccumulationValue, AccumulationStore> result =
        super.visitMethodInvocation(methodInvocationNode, in);

    // Assume any receiver or argument involved in
    // a boolean method invocation to be validated
    if (aTypeFactory.enableValidationCheck) {
      if (SymbolUtils.isMethodInvocationInIfConditional(methodInvocationNode.getTreePath())
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
    if (aTypeFactory.unannotatedCodeHandlingEnabled() && aTypeFactory.enableSideEffect) {
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
      MethodInvocationTree tree,
      TransferResult<AccumulationValue, AccumulationStore> result,
      MethodInvocationNode node,
      boolean isTainted) {
    AnnotatedTypeMirror rAnno = aTypeFactory.getAnnotatedType(node.getTree());
    Symbol.MethodSymbol symbol = (Symbol.MethodSymbol) TreeUtils.elementFromUse(tree);
    if (aTypeFactory.isUnannotatedMethod(symbol) || rAnno.hasPrimaryAnnotation(RThis.class)) {
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
    if (!methodInvocationNode.getArguments().isEmpty()) {
      Node receiver = methodInvocationNode.getTarget().getReceiver();
      if (receiver != null && !(receiver instanceof ImplicitThisNode)) {
        return receiver instanceof LocalVariableNode
            || receiver instanceof FieldAccessNode
            || receiver instanceof MethodInvocationNode;
      }
    }
    return false;
  }

  private void makePossiblyValidated(
      TransferResult<AccumulationValue, AccumulationStore> result, Node node, Node calledMethod) {
    AnnotatedTypeMirror type = aTypeFactory.getAnnotatedType(node.getTree());
    JavaExpression je = JavaExpression.fromNode(node);
    List<String> calledMethods;

    if (type.hasPrimaryAnnotation(aTypeFactory.rTainted)) {
      calledMethods = Collections.singletonList(calledMethod.toString());
      insertOrRefineRPossiblyValidated(result, je, calledMethods);
    } else if (type.getPrimaryAnnotation() != null
        && aTypeFactory.isAccumulatorAnnotation(type.getPrimaryAnnotation())) {
      calledMethods = aTypeFactory.getAccumulatedValues(type.getPrimaryAnnotation());
      calledMethods.add(calledMethod.toString());
      insertOrRefineRPossiblyValidated(result, je, calledMethods);
    }
  }

  private void insertOrRefineRPossiblyValidated(
      TransferResult<AccumulationValue, AccumulationStore> result,
      JavaExpression je,
      List<String> calledMethods) {
    result.getThenStore().insertOrRefine(je, aTypeFactory.rPossiblyValidatedAM(calledMethods));
    result.getElseStore().insertOrRefine(je, aTypeFactory.rPossiblyValidatedAM(calledMethods));
  }

  private void makeStoresTainted(
      TransferResult<AccumulationValue, AccumulationStore> result, Node n) {
    if (n.getTree() == null) {
      return;
    }
    AnnotatedTypeMirror type = aTypeFactory.getAnnotatedType(n.getTree());
    if (type.hasPrimaryAnnotation(aTypeFactory.rUntainted)) {
      JavaExpression je = JavaExpression.fromNode(n);

      if (!je.containsUnknown()) {
        AccumulationValue thenVal = result.getThenStore().getValue(je);
        if (thenVal != null) {
          thenVal =
              analysis.createAbstractValue(
                  AnnotationMirrorSet.singleton(aTypeFactory.rTainted),
                  thenVal.getUnderlyingType());
          result.getThenStore().replaceValue(je, thenVal);
        }

        AccumulationValue elseVal = result.getElseStore().getValue(je);
        if (elseVal != null) {
          elseVal =
              analysis.createAbstractValue(
                  AnnotationMirrorSet.singleton(aTypeFactory.rTainted),
                  elseVal.getUnderlyingType());
          result.getThenStore().replaceValue(je, elseVal);
        }
      }
    }
  }
}
