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

package edu.ucr.cs.riple.taint.ucrtainting;

import com.sun.tools.javac.tree.JCTree;
import edu.ucr.cs.riple.taint.ucrtainting.serialization.SerializationService;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.accumulation.AccumulationChecker;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SupportedOptions;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/** This is the entry point for pluggable type-checking. */
@StubFiles({
  "stubs/apache.commons.io.astub",
  "stubs/apache.commons.lang.astub",
  "stubs/codeql.astub",
  "stubs/Connection.astub",
  "stubs/Files.astub",
  "stubs/find-sec-bugs-sanitizers.astub",
  "stubs/general.astub",
  "stubs/httpservletreq.astub",
  //  "stubs/StringBuffer.astub",
  "stubs/taintedMethods.astub",
  "stubs/tdmljp.astub",
})
@SupportedOptions({
  UCRTaintingChecker.ANNOTATED_PACKAGES,
  UCRTaintingChecker.ENABLE_LIBRARY_CHECKER,
  UCRTaintingChecker.ENABLE_VALIDATION_CHECKER,
  UCRTaintingChecker.ENABLE_SIDE_EFFECT,
  UCRTaintingChecker.ENABLE_POLY_TAINT_INFERENCE,
  UCRTaintingChecker.ENABLE_TYPE_ARGUMENT_INFERENCE,
  Config.SERIALIZATION_CONFIG_PATH,
  Config.SERIALIZATION_ACTIVATION_FLAG,
})
public class UCRTaintingChecker extends AccumulationChecker {

  public static int index = 0;
  public static final String ENABLE_VALIDATION_CHECKER = "enableValidationCheck";
  public static final String ENABLE_LIBRARY_CHECKER = "enableLibraryCheck";
  public static final String ENABLE_POLY_TAINT_INFERENCE = "enablePolyTaintInference";
  public static final String ENABLE_TYPE_ARGUMENT_INFERENCE = "enableTypeArgumentInference";
  public static final String ENABLE_SIDE_EFFECT = "enableSideEffect";
  public static final String ANNOTATED_PACKAGES = "annotatedPackages";
  /** Serialization service for the checker. */
  private SerializationService serializationService;

  private UCRTaintingAnnotatedTypeFactory typeFactory;
  private boolean serialize = true;
  private FoundRequired pair = null;

  public UCRTaintingChecker() {}

  @Override
  public void initChecker() {
    super.initChecker();
    this.serializationService = new SerializationService(this);
    this.typeFactory = (UCRTaintingAnnotatedTypeFactory) getTypeFactory();
  }

  @Override
  public void reportError(Object source, @CompilerMessageKey String messageKey, Object... args) {
    pair = pair == null ? retrievePair(messageKey, args) : pair;
    if (serialize) {
      this.serializationService.serializeError(source, messageKey, pair);
    }
    super.reportError(source, messageKey, args);
  }

  public void detailedReportError(
      Object source, @CompilerMessageKey String messageKey, FoundRequired pair, Object... args) {
    this.serializationService.serializeError(source, messageKey, pair);
    this.serialize = false;
    this.pair = pair;
    this.reportError(source, messageKey, args);
    this.pair = null;
    this.serialize = true;
  }

  private FoundRequired retrievePair(String messageKey, Object... args) {
    switch (messageKey) {
      case "override.return":
        {
          AnnotatedTypeMirror.AnnotatedExecutableType overriddenType =
              (AnnotatedTypeMirror.AnnotatedExecutableType) args[5];
          AnnotatedTypeMirror.AnnotatedExecutableType overridingType =
              (AnnotatedTypeMirror.AnnotatedExecutableType) args[3];
          return FoundRequired.of(
              overridingType.getReturnType(), overriddenType.getReturnType(), 0);
        }
      case "override.param":
        {
          AnnotatedTypeMirror.AnnotatedExecutableType overriddenType =
              (AnnotatedTypeMirror.AnnotatedExecutableType) args[6];
          AnnotatedTypeMirror.AnnotatedExecutableType overridingType =
              (AnnotatedTypeMirror.AnnotatedExecutableType) args[4];
          int index = 0;
          JCTree.JCMethodDecl i = (JCTree.JCMethodDecl) getVisitor().getCurrentPath().getLeaf();
          for (JCTree.JCVariableDecl arg : i.getParameters()) {
            if (arg.getName().toString().equals(args[0])) {
              break;
            }
            index++;
          }
          return FoundRequired.of(
              overriddenType.getParameterTypes().get(index),
              overridingType.getParameterTypes().get(index),
              0);
        }
      default:
        return null;
    }
  }
}
