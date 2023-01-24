package thirdPartyTests;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;

class StubMethodsTest {
  @RTainted String taintedStr = "taintedData";
  @RUntainted String untaintedStr = "untaintedData";

  void passUntaintedParamToSystemLoad() {
    System.load(untaintedStr);
  }

  void passTaintedParamToSystemLoad() {
    // :: error: (argument)
    System.load(taintedStr);
  }

  void passUntaintedParamToProcessExec() {
    try {
      Runtime rt = Runtime.getRuntime();
      Process pr = rt.exec(untaintedStr);
    } catch (Exception e) {
      // Do nothing
    }
  }

  void passTaintedParamToProcessExec() {
    try {
      Runtime rt = Runtime.getRuntime();
      // :: error: (argument)
      Process pr = rt.exec(taintedStr);
    } catch (Exception e) {
      // Do nothing
    }
  }
}
