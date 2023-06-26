package test;

import com.vaadin.shared.ui.ContentMode;
import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;
import javax.servlet.http.*;
import org.safehaus.uuid.UUID;

public class CmsUUID {

  /** Internal UUID implementation. */
  private transient UUID m_uuid;
  /** Constant for the null UUID. */
  private static final @RUntainted CmsUUID NULL_UUID = new CmsUUID(UUID.getNullUUID());

  private static CmsUUID cms;

  private static final String BAZ = "property";
  private static final String PATH = "/";

  //  // Should not be error here.
  static final int CONCURRENCY_LEVEL = 8;
  public static final List<@RUntainted String> FOLDERS =
      Collections.unmodifiableList(Arrays.asList(BAZ));

  public CmsUUID(UUID uuid) {
    m_uuid = uuid;
  }

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
    // :: error: assignment
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
      // :: error: assignment
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
}
