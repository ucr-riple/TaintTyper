package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.awt.Component;
import java.util.HashMap;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.swing.Icon;
import javax.swing.JOptionPane;

public class Download {

  static LocaleManager S = new LocaleManager("resources/logisim", "fpga");

  public static String chooseBoard(List<@RUntainted String> devices) {
    /* This code is based on the version of Kevin Walsh */
    if (Main.hasGui()) {
      java.lang.String[] choices = new String[devices.size()];
      for (int i = 0; i < devices.size(); i++) choices[i] = devices.get(i);
      return (String)
          OptionPane.showInputDialog(
              null,
              S.get("FPGAMultipleBoards", devices.size()),
              S.get("FPGABoardSelection"),
              OptionPane.QUESTION_MESSAGE,
              null,
              choices,
              choices[0]);
    } else {
      /* TODO: add none gui selection */
      return null;
    }
  }

  static class Main {
    static boolean hasGui() {
      return false;
    }
  }

  static class OptionPane {
    public static Object showInputDialog(
        Component parentComponent,
        Object message,
        String title,
        int messageType,
        Icon icon,
        Object[] selectionValues,
        Object initialSelectionValue) {
      return Main.hasGui()
          ? JOptionPane.showInputDialog(
              parentComponent,
              message,
              title,
              messageType,
              icon,
              selectionValues,
              initialSelectionValue)
          : null;
    }

    public static int QUESTION_MESSAGE = 0;
  }

  static class LocaleManager {

    private ResourceBundle locale = null;
    private static final HashMap<Character, String> repl = null;

    public LocaleManager(String string, String string2) {}

    public String get(String key, Object... args) {
      return String.format(get(key), args);
    }

    public String get(String key) {
      String ret;
      try {
        ret = locale.getString(key);
      } catch (MissingResourceException e) {
        ret = key;
      }
      final java.util.HashMap<java.lang.Character, java.lang.String> repl = LocaleManager.repl;
      if (repl != null) ret = replaceAccents(ret, repl);
      return ret;
    }

    private static String replaceAccents(String src, HashMap<Character, String> repl) {
      // find first non-standard character - so we can avoid the
      // replacement process if possible
      int i = 0;
      int n = src.length();
      for (; i < n; i++) {
        final char ci = src.charAt(i);
        if (ci < 32 || ci >= 127) break;
      }
      if (i == n) return src;

      // ok, we'll have to consider replacing accents
      char[] cs = src.toCharArray();
      final java.lang.StringBuilder ret = new StringBuilder(src.substring(0, i));
      for (int j = i; j < cs.length; j++) {
        char cj = cs[j];
        if (cj < 32 || cj >= 127) {
          String out = repl.get(cj);
          if (out != null) {
            ret.append(out);
          } else {
            ret.append(cj);
          }
        } else {
          ret.append(cj);
        }
      }
      return ret.toString();
    }
  }
}
