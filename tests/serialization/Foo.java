import edu.ucr.cs.riple.taint.ucrtainting.qual.*;

class Foo {

  Object field = new Object();
  Bar bar = new Bar();

  void bar(Object x, @RUntainted Object y, boolean b) {
    Object localVar = x;
    final Object finalLocalVar = x;
    // :: error: assignment
    @RUntainted Object c = field;
    // :: error: assignment
    c = x;
    // :: error: assignment
    c = b ? x : y;
    if (b) {
      // :: error: assignment
      c = bar.getField();
    } else {
      // :: error: assignment
      c = bar.field;
      // :: error: assignment
      c = x;
      // :: error: assignment
      c = localVar;
      // :: error: assignment
      c = bar.baz.field;
    }
    class LocalClass {
      @RUntainted Object field;
      Object f2;

      void foo() {
        // :: error: assignment
        field = finalLocalVar;
        // :: error: assignment
        field = f2;
      }
    }
    // :: error: assignment
    c = bar.staticF;
  }
}
