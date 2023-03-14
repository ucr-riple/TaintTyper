import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.util.*;

public class Foo<E> {

  public HashMap<E, String> map;

  public E getE() {
    return map.keySet().iterator().next();
  }

  public String getString() {
    return "";
  }
}
