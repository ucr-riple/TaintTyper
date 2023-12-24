package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.List;
import org.apache.commons.text.StringSubstitutor;

public class Recursive {

  interface Lookup {
    Object lookup(String variable);
  }

  public void test() {
    // :: error: assignment
    @RUntainted String interpolated = interpolate("foo");
  }

  protected String interpolate(final String base) {
    final Object result = interpolate((Object) base);
    return result == null ? null : result.toString();
  }

  protected Object interpolate(final Object value) {
    final ConfigurationInterpolator ci = null;
    return ci != null ? ci.interpolate(value) : value;
  }

  class ConfigurationInterpolator {

    private final StringSubstitutor substitutor = null;
    private static final char PREFIX_SEPARATOR = ':';
    private final List<Lookup> defaultLookups = List.of();

    private static String extractVariableName(final String strValue) {
      return strValue.substring(0);
    }

    private Object resolveSingleVariable(final String strValue) {
      return resolve(extractVariableName(strValue));
    }

    public Object interpolate(final Object value) {
      if (value instanceof String) {
        final String strValue = (String) value;
        if (isSingleVariable(strValue)) {
          final Object resolvedValue = resolveSingleVariable(strValue);
          if (resolvedValue != null && !(resolvedValue instanceof String)) {
            return resolvedValue;
          }
        }
        return substitutor.replace(strValue);
      }
      return value;
    }

    private boolean isSingleVariable(final String strValue) {
      return false;
    }

    public Object resolve(final String var) {
      if (var == null) {
        return null;
      }
      final int prefixPos = var.indexOf(PREFIX_SEPARATOR);
      if (prefixPos >= 0) {
        final String prefix = var.substring(0, prefixPos);
        final String name = var.substring(prefixPos + 1);
        final Object value = name;
        if (value != null) {
          return value;
        }
      }
      for (final Lookup lookup : defaultLookups) {
        final Object value = lookup.lookup(var);
        if (value != null) {
          return value;
        }
      }
      return resolve(var);
    }
  }

  public void testRecurs() {
    // :: error: assignment
    @RUntainted Object ans = recurs("foo");
  }

  public String recurs(Object value) {
    if (value instanceof String) {
      return recurs(value);
    }
    return recurs(value) + value;
  }

  Lookup lookup;

  public String pol1(String param1, String param2) {
    return pol2(param1, param2) + param1 + lookup.lookup(param1);
  }

  public String pol2(String param1, String param2) {
    return pol1(param1, param2) + param2 + lookup.lookup(param2);
  }

  public void testMutalRecursion() {
    // :: error: assignment
    @RUntainted String interpolated = pol1("foo", "bar");
  }
}
