import edu.xxx.cs.yyyyy.taint.tainttyper.qual.*;
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
