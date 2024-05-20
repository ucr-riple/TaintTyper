package foo.bar;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.Map.Entry;
import javax.servlet.http.*;

public class Foo {

  private List<String> tokens;

  public void testMap(String param) {
    Map<String, String> map = new HashMap<>();
    // :: error: assignment
    @RUntainted String s = map.get(param);
  }

  public void testMapOfList(List<String> key) {
    Map<List<String>, List<String>> mapOfList = new HashMap<>();
    // :: error: assignment
    @RUntainted String s = mapOfList.get(key).get(0);
  }

  public void testGenericFoo() {
    GenericFoo<String, String> gen = new GenericFoo<>();
    // :: error: assignment
    @RUntainted String s = gen.bar.getM();
    // :: error: assignment
    s = gen.bar.getL();
  }

  public void testGenericBar() {
    GenericBar<String, Map<String, String>> gen = new GenericBar<>();
    // :: error: assignment
    @RUntainted String s = gen.instance().bar.getM();
    // :: error: assignment
    s = gen.instanceWithL().getK().keySet().iterator().next();
    // :: error: assignment
    s = gen.instanceWithL().getK().values().iterator().next();
    // :: error: assignment
    Iterator<@RUntainted String> iter = gen.instanceWithL().getK().keySet().iterator();
  }

  public void rawUsedTypes(Item item1) {
    // :: error: assignment
    @RUntainted String name1 = (String) (item1.getItemProperty(null).getValue());
  }

  public @RUntainted String sameTypeVarWithDifferentOwners(String galleryType) {
    SortedMap<String, String> m_startGalleriesSettings = null;
    // :: error: return
    return m_startGalleriesSettings.get(galleryType);
  }

  public void containingTypeParameterNotShownInReceivers() {
    Iterator<Entry<String, String>> itEntries = null;
    // :: error: assignment
    @RUntainted Entry<@RUntainted String, @RUntainted String> entry = itEntries.next();
  }

  public void enhancedForLeftHandSide(Set<Map.Entry<String, @RUntainted String>> entrySet) {
    // :: error: enhancedfor
    for (Map.Entry<String, String> entry : entrySet) {}
  }

  public void testOnClassDeclarationChange() {
    // :: error: assignment
    @RUntainted String s1 = AccessController.doPrivilegedInterface(new SystemPropertyAction());
    // :: error: assignment
    @RUntainted String s2 = AccessController.doPrivilegedSuperClass(new SystemPropertyAction());
  }

  static class AccessController {
    public static <T> T doPrivilegedInterface(PrivilegedActionInterface<T> action) {
      return null;
    }

    public static <T> T doPrivilegedSuperClass(PrivilegedActionSuperClass<T> action) {
      return null;
    }
  }

  static class SystemPropertyAction extends PrivilegedActionSuperClass<String>
      implements PrivilegedActionInterface<String> {}

  static interface PrivilegedActionInterface<T> {}

  static class PrivilegedActionSuperClass<T> {}

  static class GenericFoo<T, K> {
    GenericBar<T, T> bar;

    K k;

    GenericBar<T, T> getBar() {
      return bar;
    }

    public K getK() {
      return k;
    }
  }

  static class GenericBar<M, L> {
    M getM() {
      return null;
    }

    L getL() {
      return null;
    }

    GenericFoo<M, L> instanceWithL() {
      return null;
    }

    GenericFoo<M, String> instance() {
      return null;
    }

    public void internal() {
      GenericBar<M, L> gen = new GenericBar<>();
      // :: error: assignment
      @RUntainted M m = gen.getM();
    }
  }

  public interface Item {
    public Property getItemProperty(Object id);
  }

  public interface Property<T> {
    public T getValue();
  }

  static class SystemPropertyActionThirdParty implements PrivilegedAction<@RUntainted String> {

    @Override
    // :: error: override.return
    public String run() {
      return null;
    }
  }

  private UserMapper userMapper;

  public @RUntainted User selectByToken(String token) {
    // :: error: return
    return userMapper.selectOne(null);
  }

  public List<@RUntainted String> testThisIdentifier() {
    // :: error: return
    return this.tokens;
  }

  public void iteratorOnMapEntryTest(Map<String, List<String>> headers) {
    Iterator<Map.Entry<@RUntainted String, @RUntainted List<@RUntainted String>>> i =
        // :: error: assignment
        headers.entrySet().iterator();
  }
}

class User {}

interface UserMapper extends BaseMapper<User> {
  int countToday();
}
