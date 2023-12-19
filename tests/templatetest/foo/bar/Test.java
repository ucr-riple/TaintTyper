package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import org.apache.commons.text.StringSubstitutor;
import java.util.List;

public class Test {

  // This exposes a bug in CF
  //    private <T> T findFirst(final Function<? extends X, ? extends T> mapper) {
  //        return null;
  //    }
  //    public @RUntainted String test() {
  //        return findFirst(X::name);
  //    }
  //
  //    class X {
  //        public String name() {
  //            return "";
  //        }
  //    }

    public void test(){
        @RUntainted String interpolated = interpolate("foo");
    }

    protected String interpolate(final String base) {
        final Object result = interpolate((Object)base);
        return result == null ? null : result.toString();
    }

    protected Object interpolate(final Object value) {
        final ConfigurationInterpolator ci = null;
        return ci != null ? ci.interpolate(value) : value;
    }
    class ConfigurationInterpolator{

        private final StringSubstitutor substitutor = null;

        private static final char PREFIX_SEPARATOR = ':';
        private final List<Lookup> defaultLookups = List.of();

        private ConfigurationInterpolator parentInterpolator;

        private static String extractVariableName(final String strValue) {
            return strValue.substring(0);
        }

        private Object resolveSingleVariable(final String strValue) {
            return resolve(extractVariableName(strValue));
        }
        public Object interpolate(final Object value) {
            if (value instanceof String) {
                final String strValue = (String)value;
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
                ///////////////////
                final Object value = "fetchLookupForPrefix(prefix).lookup(name)";
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

            final ConfigurationInterpolator parent = getParentInterpolator();
            if (parent != null) {
                return getParentInterpolator().resolve(var);
            }
            return null;
        }

        /**
         * Gets the parent {@code ConfigurationInterpolator}.
         *
         * @return the parent {@code ConfigurationInterpolator} (can be <b>null</b>)
         */
        public ConfigurationInterpolator getParentInterpolator() {
            return this.parentInterpolator;
        }
    }

    interface Lookup {
        /**
         * Looks up the value of the specified variable. This method is called by {@link ConfigurationInterpolator} with the
         * variable name extracted from the expression to interpolate (i.e. the prefix name has already been removed). A
         * concrete implementation has to return the value of this variable or <b>null</b> if the variable name is unknown.
         *
         * @param variable the name of the variable to be resolved
         * @return the value of this variable or <b>null</b>
         */
        Object lookup(String variable);
    }
}
