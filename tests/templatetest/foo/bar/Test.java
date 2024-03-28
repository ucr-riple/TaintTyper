package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;

public class Test {

  //    @SuppressWarnings("removal")
  //    void test(){
  //        // :: error: argument
  //        // :: error: assignment
  //        @RUntainted File uriFile = new File(AccessController.doPrivileged(new
  // PrivilegedAction<String>() {
  //            @Override
  //            public String run() {
  //                return null;
  //            }
  //        }));
  //    }
}
