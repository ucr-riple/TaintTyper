package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.lang.annotation.*;
import java.util.*;

public class Test {

  //    public void test(Map<Map<List<String>, String>, List<String>> map){
  //        for (Map<List<String>, String> smallMap : map.keySet()) {
  //            for(Map.Entry<List<String>, String> entry : smallMap.entrySet()){
  //                for(String s : entry.getKey()){
  //                    // :: error: argument
  //                    sink(s);
  //                }
  //            }
  //        }
  //    }
  //
  //    void sink(@RUntainted String s){
  //
  //    }
}
