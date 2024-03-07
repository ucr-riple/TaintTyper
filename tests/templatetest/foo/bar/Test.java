package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;

public class Test {

  ////    List<String[]> list;
  //
  //    void exec(){
  //// //     :: error: argument
  ////        sink(source()[0]);
  //
  //        String @RUntainted [] s = source();
  //    }
  //
  //    String[] source(){
  //        return new String[1];
  //    }
  //
  //    void sink(@RUntainted String s){
  //
  //    }
  //
  //    public void arrayCopy(String[] existedProperties) {
  //        int epl = existedProperties.length;
  //        // :: error: assignment
  //        @RUntainted String[] newProperties = Arrays.copyOf(existedProperties, epl + 1);
  //    }
}
