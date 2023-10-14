package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;
import java.util.Collections;

public class Foo {

  private static final String BASIC_BUILDER =
      "org.apache.commons.configuration2.builder.BasicConfigurationBuilder";

  /** Constant for the provider for JNDI sources. */
  private static final BaseConfigurationBuilderProvider JNDI_PROVIDER =
      new BaseConfigurationBuilderProvider(
          BASIC_BUILDER,
          null,
          "org.apache.commons.configuration2.JNDIConfiguration",
          Collections.singletonList(
              "org.apache.commons.configuration2.builder.JndiBuilderParametersImpl"));

  static class BaseConfigurationBuilderProvider {
    public BaseConfigurationBuilderProvider(
        String BASIC_BUILDER, Object object, String s, List<String> singletonList) {}
  }
}
