package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import thirdparty.FileSystem;

public class Main {

    void foo(String y) {
        // //         :: error: argument
        @RUntainted FileSystem fs = new FileSystem(null);
    }
}
