package test;

import com.vaadin.shared.ui.ContentMode;
import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;
import javax.servlet.http.*;
import org.safehaus.uuid.UUID;

public class CmsUUID {

  /** Internal UUID implementation. */
  private transient UUID m_uuid;
  /** Constant for the null UUID. */
  private static final @RUntainted CmsUUID NULL_UUID = new CmsUUID(UUID.getNullUUID());

  private static @RUntainted CmsUUID cms;

  private static final String BAZ = "property";
  private static final String PATH = "/";

  // Should not be error here.
  static final int CONCURRENCY_LEVEL = 8;
  public static final List<@RUntainted String> FOLDERS =
      Collections.unmodifiableList(Arrays.asList(BAZ));

  public CmsUUID(UUID uuid) {
    m_uuid = uuid;
  }

  public static final String IMAGE_MIMETYPECONFIG = "png:image/png|gif:image/gif|jpg:image/jpeg";
  public static final Map<String, @RUntainted String> IMAGE_MIMETYPES =
      Collections.unmodifiableMap(splitAsMap(IMAGE_MIMETYPECONFIG, "|", ":"));

  public static CmsUUID create(UUID uid) {
    return new CmsUUID(uid);
  }

  @RUntainted
  Object castTest() {
    if (this == NULL_UUID) {
      return NULL_UUID;
    }
    // :: error: return
    return new CmsUUID((UUID) m_uuid.clone());
  }

  void binaryExpressionTest() {
    // :: error: assignment
    @RUntainted boolean isDefault = (m_uuid != null) && Boolean.valueOf(m_uuid.toString());
  }

  void enumFromThirdPartyTest() {
    // should not be error here.
    @RUntainted ContentMode mode = ContentMode.HTML;
  }

  @RUntainted
  CmsUUID staticCallTest(UUID uid) {
    // :: error: return
    return CmsUUID.create(uid);
  }

  protected @RUntainted CmsPair<@RUntainted String, @RUntainted String> decode(
      String content, String encoding) {
    // :: error: return
    return CmsPair.create(content, encoding);
  }

  public void testFinalStaticString() {
    // should not be error here.
    @RUntainted String foo1 = BAZ;
    // should not be error here.
    @RUntainted String foo2 = CmsUUID.BAZ;
  }

  public enum BundleType {
    PROPERTY(cms);

    BundleType(CmsUUID s) {}

    public static @RUntainted BundleType toBundleType(String value) {

      if (null == value) {
        return null;
      }
      if (value.equals(PROPERTY.toString())) {
        // Should not get error here.
        return PROPERTY;
      }
      return null;
    }
  }

  public void testConstantForThirdpartyArgument() {
    @RUntainted List<String> list = new ArrayList<>();
    // :: error: (assignment)
    @RUntainted String s = list.get(0);
  }

  public void testMemberSelectOfFinalStatic(HttpServletResponse response) {
    sink(Boolean.TRUE);
  }

  public void bar() {
    try {
      // some code
    } catch (Exception e) {
      // Should not try to annotate "e" here.
      @RUntainted Exception dup = e;
    }
  }

  public void sink(@RUntainted boolean b) {}

  public void multipleAdditionTest() {
    class XMLPage {
      public String getRootPath() {
        return "";
      }
    }
    @RUntainted String path = "some path";
    String[] tokens = path.split("/");
    // :: error: assignment
    @RUntainted String name = tokens[1];
    XMLPage xmlPage = new XMLPage();
    @RUntainted
    String fullPath =
        // :: error: assignment
        xmlPage.getRootPath() + "/" + tokens[0] + "/" + name + "." + BAZ;
  }

  public void testUntaintedForAnyFinalStaticWithInitializer() {
    for (String folder : FOLDERS) {
      // Should not be an error here.
      @RUntainted String f = folder;
    }

    for (String folder : CmsUUID.FOLDERS) {
      // Should not be an error here.
      @RUntainted String f = folder;
    }
  }

  public static @RUntainted Map<@RUntainted String, @RUntainted String> splitAsMap(
      String source, String paramDelim, String keyValDelim) {

    int keyValLen = keyValDelim.length();
    // use LinkedHashMap to preserve the order of items
    Map<@RUntainted String, @RUntainted String> params =
        new LinkedHashMap<@RUntainted String, @RUntainted String>();
    Iterator<String> itParams = splitAsList(source, paramDelim, true).iterator();
    while (itParams.hasNext()) {
      String param = itParams.next();
      int pos = param.indexOf(keyValDelim);
      String key = param;
      String value = "";
      if (pos > 0) {
        key = param.substring(0, pos);
        if ((pos + keyValLen) < param.length()) {
          value = param.substring(pos + keyValLen);
        }
      }
      // :: error: argument
      params.put(key, value);
    }
    return params;
  }

  public static List<String> splitAsList(String source, String delimiter, boolean trim) {

    int dl = delimiter.length();

    List<String> result = new ArrayList<String>();
    int i = 0;
    int l = source.length();
    int n = source.indexOf(delimiter);
    while (n != -1) {
      if ((i < n) || ((i > 0) && (i < l))) {
        result.add(trim ? source.substring(i, n).trim() : source.substring(i, n));
      }
      i = n + dl;
      n = source.indexOf(delimiter, i);
    }
    // is there a non - empty String to cut from the tail?
    if (n < 0) {
      n = source.length();
    }
    if (i < n) {
      result.add(trim ? source.substring(i).trim() : source.substring(i));
    }
    return result;
  }

  public void testOnTypeArgChangeOnMethodAsReceiver() {
    // :: error: assignment
    Iterator<@RUntainted String> itParams = splitAsList(null, null, true).iterator();
  }

  public void testValueForStaticFinalMap() {
    @RUntainted String s = IMAGE_MIMETYPES.get("png");
  }

  public void recentCrash() {
    List<String> params = new LinkedList<String>();
    // :: error: argument
    ProcessBuilder pb = new ProcessBuilder(params.toArray(new String[params.size()]));
  }

  public void zeroArgMethodCall(HttpServletRequest request) {

    @RUntainted CmsUgcSession session = createSession();
    HttpSession httpSession = request.getSession(true);
    // :: error: argument
    httpSession.setAttribute("" + session.getId(), session);
  }

  public static @RUntainted CmsUgcSession createSession() {
    return new CmsUgcSession();
  }

  static class CmsUgcSession {

    String id;

    public String getId() {
      return id;
    }
  }

  public void checkToArrayForCustomList() {
    abstract class CustomList<R, V> implements Collection<V> {}
    class Custom<H, U> extends CustomList<U, H> {
      public int size() {
        return 0;
      }

      public boolean isEmpty() {
        return false;
      }

      public boolean contains(Object o) {
        return false;
      }

      public Iterator<H> iterator() {
        return null;
      }

      public Object[] toArray() {
        return new Object[0];
      }

      public <T> T[] toArray(T[] a) {
        return null;
      }

      public boolean add(H h) {
        return false;
      }

      public boolean remove(Object o) {
        return false;
      }

      public boolean containsAll(Collection<?> c) {
        return false;
      }

      public boolean addAll(Collection<? extends H> c) {
        return false;
      }

      public boolean removeAll(Collection<?> c) {
        return false;
      }

      public boolean retainAll(Collection<?> c) {
        return false;
      }

      public void clear() {}
    }
    Custom<String, String> params = new Custom<>();
    // :: error: argument
    ProcessBuilder pb = new ProcessBuilder(params.toArray(new String[params.size()]));
  }

  void testAnnotationComponentType(DataBinding binding) {
    sink(binding.classes()[0]);
  }

  void sink(@RUntainted Object s) {}

  @SuppressWarnings("unchecked")
  private <T> @RUntainted Class<? extends T>[] testSkipReportForAnnotationMemberSelection(
      Annotation ann, Class<T> type) { // NOPMD
    if (ann instanceof DataBinding) {
      return (Class<? extends T>[]) ((DataBinding) ann).classes();
    }
    throw new UnsupportedOperationException("Doesn't support the annotation: " + ann);
  }
}

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
@interface DataBinding {
  Class<?>[] classes();

  /**
   * Bean reference to lookup in configuration. Bean must be castable to the Class set above
   *
   * @return The id of the bean reference
   */
  String ref() default "";
}
