package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

public class Test {

    public void test(Foo<String, String> foo){
        // :: error: enhancedfor
        for(@RUntainted String s : foo){

        }
    }


    class Foo<T, M> extends ArrayList<M>{

    }
}
