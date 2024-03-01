package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;

public class Test {

  //    private static final Pattern PROPERTY_PATTERN = Pattern.compile("([^=]+)(=)(.*)");
  //    private static final int IDX_KEY = 1;
  //    private static final int IDX_SEPARATOR = 2;
  //    private static final int IDX_VALUE = 3;
  //
  //
  //    static @RPolyTainted String[] doParseProperty(final @RPolyTainted String line, final boolean
  // trimValue) {
  //        final Matcher matcher = PROPERTY_PATTERN.matcher(line);
  //        final @RUntainted String[] result = {"", "", ""};
  //        if (matcher.matches()) {
  //            result[0] = matcher.group(IDX_KEY).trim();
  //            String value = matcher.group(IDX_VALUE);
  //            if (trimValue) {
  //                value = value.trim();
  //            }
  //            result[1] = value;
  //            result[2] = matcher.group(IDX_SEPARATOR);
  //        }
  //        return result;
  //    }
}
