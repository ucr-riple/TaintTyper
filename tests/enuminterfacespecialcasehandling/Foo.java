package test;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;
import javax.servlet.http.*;

public class Foo {

  public static final String BIG_ICON_SUFFIX = "_big_icon.png";
  /** The small icon suffix. */
  public static final String SMALL_ICON_SUFFIX = "_small_icon.png";
  /** The tiny icon suffix. */
  public static final String TINY_ICON_SUFFIX = "_tiny_icon.png";

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

  private enum IconSize {
    Big(96, BIG_ICON_SUFFIX),
    Small(32, SMALL_ICON_SUFFIX),
    Tiny(23, TINY_ICON_SUFFIX);
    private int m_size;
    private String m_suffix;

    private IconSize(int size, String suffix) {
      m_size = size;
      m_suffix = suffix;
    }
  }

  static String taintedStatic = null;

  public enum CodeMirrorLanguage {
    CSS("css", new String[] {"css"}),
    // :: error: enum.declaration
    XML("application/xml", new String[] {"xml", taintedStatic});
    private final String m_languageName;
    private Set<String> m_supportedFileTypes;

    private CodeMirrorLanguage(String languageName, String[] fileTypes) {
      m_languageName = languageName;
      m_supportedFileTypes = new HashSet<String>(Arrays.asList(fileTypes));
    }
  }
}
