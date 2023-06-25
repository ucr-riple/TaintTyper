package test;

import com.vaadin.shared.ui.ContentMode;
import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;
import org.safehaus.uuid.UUID;

public class CmsUUID {

  /** Internal UUID implementation. */
  private transient UUID m_uuid;
  /** Constant for the null UUID. */
  private static final @RUntainted CmsUUID NULL_UUID = new CmsUUID(UUID.getNullUUID());

  private static CmsUUID cms;

  public CmsUUID(UUID uuid) {
    m_uuid = uuid;
  }

  public static CmsUUID create(UUID uid) {
    return new CmsUUID(uid);
  }

//  @RUntainted
//  Object castTest() {
//    if (this == NULL_UUID) {
//      return NULL_UUID;
//    }
//    // :: error: return
//    return new CmsUUID((UUID) m_uuid.clone());
//  }
//
//  void binaryExpressionTest() {
//    // :: error: assignment
//    @RUntainted boolean isDefault = (m_uuid != null) && Boolean.valueOf(m_uuid.toString());
//  }
//
//  void enumFromThirdPartyTest() {
//    // should not be error here.
//    @RUntainted ContentMode mode = ContentMode.HTML;
//  }
//
//  @RUntainted
//  CmsUUID staticCallTest(UUID uid) {
//    // :: error: return
//    return CmsUUID.create(uid);
//  }

  protected @RUntainted CmsPair<@RUntainted String, @RUntainted String> decode(
      String content, String encoding) {
    // :: error: return
    return CmsPair.create(content, encoding);
  }

//  public enum BundleType {
//    // :: error: assignment
//    PROPERTY(cms);
//
//    BundleType(CmsUUID s) {}
//
//    public static @RUntainted BundleType toBundleType(String value) {
//
//      if (null == value) {
//        return null;
//      }
//      if (value.equals(PROPERTY.toString())) {
//        // Should not get error here.
//        return PROPERTY;
//      }
//      return null;
//    }
//  }
}
