package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.lang.annotation.*;
import java.util.*;

public class Test {

  //    public static @RUntainted Map<@RUntainted String, @RUntainted String> splitAsMap(
  //            String source, String paramDelim, String keyValDelim) {
  //
  //        int keyValLen = keyValDelim.length();
  //        // use LinkedHashMap to preserve the order of items
  //        Map<@RUntainted String, @RUntainted String> params =
  //                new LinkedHashMap<@RUntainted String, @RUntainted String>();
  //        Iterator<String> itParams = splitAsList(source, paramDelim, true).iterator();
  //        while (itParams.hasNext()) {
  //            String param = itParams.next();
  //            int pos = param.indexOf(keyValDelim);
  //            String key = param;
  //            String value = "";
  //            if (pos > 0) {
  //                key = param.substring(0, pos);
  //                if ((pos + keyValLen) < param.length()) {
  //                    value = param.substring(pos + keyValLen);
  //                }
  //            }
  //            // :: error: argument
  //            params.put(key, value);
  //        }
  //        return params;
  //    }
  //
  public static List<String> splitAsList(String source, String delimiter, boolean trim) {
    return null;
  }

  public void testTtt() {
    // :: error: assignment
    Iterator<@RUntainted String> itParams = splitAsList(null, null, true).iterator();
  }
}
