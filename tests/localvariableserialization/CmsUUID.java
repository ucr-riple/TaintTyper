package test;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;
import org.safehaus.uuid.UUID;

public class CmsUUID {

  /** Internal UUID implementation. */
  private transient UUID m_uuid;
  /** Constant for the null UUID. */
  private static final @RUntainted CmsUUID NULL_UUID = new CmsUUID(UUID.getNullUUID());

  private CmsUUID(UUID uuid) {
    m_uuid = uuid;
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
}
