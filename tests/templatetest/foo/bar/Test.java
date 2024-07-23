package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.lang.annotation.*;
import java.util.*;

public class Test {

  void run(Message message) {
    // :: error: assignment
    @RUntainted Object uncertain = message.get(String.class);
  }
}

interface Message extends StringMap {}

interface StringMap extends Map<String, Object> {

  <T> T get(Class<T> key);

  <T> void put(Class<T> key, T value);

  <T> T remove(Class<T> key);
}
