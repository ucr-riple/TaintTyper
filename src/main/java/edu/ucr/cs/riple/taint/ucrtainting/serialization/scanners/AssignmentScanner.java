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

package edu.ucr.cs.riple.taint.ucrtainting.serialization.scanners;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import edu.ucr.cs.riple.taint.ucrtainting.FoundRequired;
import edu.ucr.cs.riple.taint.ucrtainting.UCRTaintingAnnotatedTypeFactory;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.Fix;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.visitors.FixComputer;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A scanner that accumulates the results of visiting an assignments to a variable within a method.
 */
public class AssignmentScanner extends AccumulateScanner {

  /** The target variable to find assignments to. */
  private final Symbol target;
  /** The factory to create annotated types. */
  private final UCRTaintingAnnotatedTypeFactory factory;

  public AssignmentScanner(
      Symbol target, FoundRequired pair, UCRTaintingAnnotatedTypeFactory factory) {
    super(pair);
    this.target = target;
    this.factory = factory;
  }

  @Override
  public Set<Fix> visitAssignment(AssignmentTree node, FixComputer visitor) {
    Element element = TreeUtils.elementFromUse(node.getVariable());
    if (target.equals(element)) {
      if (node.getVariable().getKind().equals(Tree.Kind.ARRAY_ACCESS)) {
        if (pair.required instanceof AnnotatedTypeMirror.AnnotatedArrayType
            && pair.found instanceof AnnotatedTypeMirror.AnnotatedArrayType) {
          AnnotatedTypeMirror.AnnotatedArrayType required =
              (AnnotatedTypeMirror.AnnotatedArrayType) pair.required;
          AnnotatedTypeMirror found = factory.getAnnotatedType(node.getExpression());
          FoundRequired newPair = FoundRequired.of(found, required.getComponentType(), pair.depth);
          return node.getExpression().accept(visitor, newPair);
        }
      }
      return node.getExpression().accept(visitor, pair);
    }
    return super.visitAssignment(node, visitor);
  }

  @Override
  public Set<Fix> visitVariable(VariableTree node, FixComputer visitor) {
    Element element = TreeUtils.elementFromDeclaration(node);
    if (target.equals(element)) {
      if (node.getInitializer() == null) {
        return Set.of();
      }
      FoundRequired newPair =
          FoundRequired.of(
              factory.getAnnotatedType(node.getInitializer()), pair.required, pair.depth);
      return node.getInitializer().accept(visitor, newPair);
    }
    return super.visitVariable(node, visitor);
  }
}
