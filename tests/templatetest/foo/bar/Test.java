package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.lang.annotation.*;
import java.util.*;

public class Test {
  //
  //    public void test(Map<String , String[]> map){
  //        // :: error: assignment
  //        @RUntainted Object[] arr = map.get("key");
  //    }
  //
  ////    public void test2(String[] arr){
  ////        Foo foo = new Foo(arr[0]);
  ////        Foo f1 = foo;
  ////        // :: error: assignment
  ////        @RUntainted Object ans = f1;
  ////
  ////    }
  //
  //    class Foo{
  //        Foo(String p){
  //
  //        }
  //    }
}
