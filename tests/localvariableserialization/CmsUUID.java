import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class CmsUUID {

  /** Internal UUID implementation. */
  private transient UUID m_uuid;
  /** Constant for the null UUID. */
  // :: error: assignment
  private static final @RUntainted CmsUUID NULL_UUID = new CmsUUID(UUID.randomUUID());

  private CmsUUID(UUID uuid) {
    m_uuid = uuid;
  }
  protected @RUntainted Object bar(){

    if (this == NULL_UUID) {
      return NULL_UUID;
    }
    // :: error: return
    return new CmsUUID((UUID)m_uuid.randomUUID());
  }
}
