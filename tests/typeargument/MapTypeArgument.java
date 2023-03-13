import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

public class MapTypeArgument<A, B, C extends Map<A, B>> {

  public C c;

  public C getC() {
    return c;
  }
}
