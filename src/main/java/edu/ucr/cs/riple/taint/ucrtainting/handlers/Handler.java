package edu.ucr.cs.riple.taint.ucrtainting.handlers;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.VariableTree;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * Interface for handlers that add annotations from default for type and visit different types of
 * trees and may modify the type of the tree.
 */
public interface Handler {

  /**
   * Adds annotations from default for the given element and type.
   *
   * @param element The element to add annotations from default for.
   * @param type The type to add annotations from default for.
   */
  void addAnnotationsFromDefaultForType(Element element, AnnotatedTypeMirror type);

  /**
   * Visits the given variable tree and may modify the type of the variable tree.
   *
   * @param variableTree The variable tree to visit.
   * @param type The type of the variable tree.
   */
  void visitVariable(VariableTree variableTree, AnnotatedTypeMirror type);

  /**
   * Visits the given method invocation tree and may modify the type of the method invocation tree.
   *
   * @param tree The method invocation tree to visit.
   * @param type The type of the method invocation tree.
   */
  void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type);

  /**
   * Visits the given member select tree and may modify the type of the member select tree.
   *
   * @param tree The member select tree to visit.
   * @param type The type of the member select tree.
   */
  void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror type);

  /**
   * Visits the given new class tree and may modify the type of the new class tree.
   *
   * @param tree The new class tree to visit.
   * @param type The type of the new class tree.
   */
  void visitNewClass(NewClassTree tree, AnnotatedTypeMirror type);

  /**
   * Visits the given lambda expression tree and may modify the type of the lambda expression tree.
   *
   * @param node The lambda expression tree to visit.
   * @param annotatedTypeMirror The type of the lambda expression tree.
   */
  void visitLambdaExpression(LambdaExpressionTree node, AnnotatedTypeMirror annotatedTypeMirror);
}
