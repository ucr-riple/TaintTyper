package test;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;
import javax.servlet.http.*;

public class Foo {
  public enum CmsReportFormatType {
    /** Default format. */
    fmtWarning("FORMAT_WARNING", IFoo.FORMAT_WARNING);

    private int m_id;
    /** The format name. */
    private String m_name;

    private CmsReportFormatType(String formatName, int formatId) {
      m_name = formatName;
      m_id = formatId;
    }
  }

  //  public void testMap(String param) {
  //    Map<String, String> map = new HashMap<>();
  //    // :: error: assignment
  //    @RUntainted String s = map.get(param);
  //  }

  public void testMapOfList(List<String> key) {
    Map<List<String>, List<@RTainted String>> mapOfList = new HashMap<>();
    // :: error: assignment
    @RUntainted String s = mapOfList.get(key).get(0);
  }
}
