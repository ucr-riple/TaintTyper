package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.nio.file.*;
import java.util.*;

public class Foo<E extends Foo<E>> {
  //
  //  public void test(@RUntainted Bar bar) {
  //    // :: error: type.argument
  //    get(bar);
  //  }
  //
  //  public void test1(String op1) {
  //    @RUntainted String s = "hello";
  //    // :: error: compound.assignment
  //    s += op1;
  //  }
  //
  //  static <E extends Foo<E>> E get(E e) {
  //    return null;
  //  }
}

class Bar extends Foo<Bar> {}

// interface ListPage<E> extends List<E>{
//
// }
//
// class ArrayListPage<E> extends ArrayList<E>{
//  public ArrayListPage(final List<E> list) {
//    super(list != null ? list : Collections.emptyList());
//  }
// }
