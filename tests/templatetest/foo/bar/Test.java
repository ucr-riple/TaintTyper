package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;

public class Test {
  //
  //  public static final PrefMonitor<@RUntainted String> QUESTA_PATH =
  //      // :: error: assignment
  //      create(new PrefMonitorString("questaPath", ""));
  //
  //  public void test(@RUntainted Option opt) {
  //    final @RUntainted String g = System.getProperty("user.home");
  //  }
  //
  //  private static <E> PrefMonitor<E> create(PrefMonitor<E> monitor) {
  //    return monitor;
  //  }
  //
  //  static class PrefMonitorString extends AbstractPrefMonitor<String> {
  //    private static boolean isSame(String a, String b) {
  //      return Objects.equals(a, b);
  //    }
  //
  //    private final String dflt;
  //
  //    private String value;
  //
  //    public PrefMonitorString(String name, String dflt) {
  //      super(name);
  //      this.dflt = dflt;
  //    }
  //
  //    public String get() {
  //      return value;
  //    }
  //
  //    public void preferenceChange(PreferenceChangeEvent event) {}
  //
  //    public void set(String newValue) {}
  //  }
  //
  //  abstract static class AbstractPrefMonitor<E> implements PrefMonitor<E> {
  //    private final String name;
  //
  //    AbstractPrefMonitor(String name) {
  //      this.name = name;
  //    }
  //
  //    public void addPropertyChangeListener(PropertyChangeListener listener) {}
  //
  //    public boolean getBoolean() {
  //      return (Boolean) get();
  //    }
  //
  //    public String getIdentifier() {
  //      return name;
  //    }
  //
  //    public boolean isSource(PropertyChangeEvent event) {
  //      return name.equals(event.getPropertyName());
  //    }
  //
  //    public void removePropertyChangeListener(PropertyChangeListener listener) {}
  //
  //    public void setBoolean(boolean value) {
  //      @SuppressWarnings("unchecked")
  //      E valObj = (E) Boolean.valueOf(value);
  //      set(valObj);
  //    }
  //  }
  //
  //  static interface PrefMonitor<E> extends PreferenceChangeListener {
  //    void addPropertyChangeListener(PropertyChangeListener listener);
  //
  //    E get();
  //
  //    boolean getBoolean();
  //
  //    String getIdentifier();
  //
  //    boolean isSource(PropertyChangeEvent event);
  //
  //    @Override
  //    void preferenceChange(PreferenceChangeEvent e);
  //
  //    void removePropertyChangeListener(PropertyChangeListener listener);
  //
  //    void set(E value);
  //
  //    void setBoolean(boolean value);
  //  }
}
