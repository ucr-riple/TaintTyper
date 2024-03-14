package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.io.IOException;
import java.io.Writer;

public class NotPolyInference {

  private static final char[] BCS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
  private static final char[] BCS_URL_SAFE =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();
  private static final char PAD = '=';

  public static char[] encodeChunk(byte[] id, int o, int l) {
    return encodeChunk(id, o, l, false);
  }

  public static char[] encodeChunk(byte[] id, int o, int l, boolean urlSafe) {
    if (id != null && id.length == 0 && l == 0) {
      return new char[0];
    } else if (l <= 0) {
      return null;
    }
    char[] out;
    if (l % 3 == 0) {
      out = new char[l / 3 * 4];
    } else {
      int finalLen = !urlSafe ? 4 : l % 3 == 1 ? 2 : 3;
      out = new char[l / 3 * 4 + finalLen];
    }
    int rindex = o;
    int windex = 0;
    int rest = l;
    final char[] base64Table = urlSafe ? BCS_URL_SAFE : BCS;
    while (rest >= 3) {
      int i =
          ((id[rindex] & 0xff) << 16) + ((id[rindex + 1] & 0xff) << 8) + (id[rindex + 2] & 0xff);
      out[windex++] = base64Table[i >> 18];
      out[windex++] = base64Table[(i >> 12) & 0x3f];
      out[windex++] = base64Table[(i >> 6) & 0x3f];
      out[windex++] = base64Table[i & 0x3f];
      rindex += 3;
      rest -= 3;
    }
    if (rest == 1) {
      int i = id[rindex] & 0xff;
      out[windex++] = base64Table[i >> 2];
      out[windex++] = base64Table[(i << 4) & 0x3f];
      if (!urlSafe) {
        out[windex++] = PAD;
        out[windex] = PAD;
      }
    } else if (rest == 2) {
      int i = ((id[rindex] & 0xff) << 8) + (id[rindex + 1] & 0xff);
      out[windex++] = base64Table[i >> 10];
      out[windex++] = base64Table[(i >> 4) & 0x3f];
      out[windex++] = base64Table[(i << 2) & 0x3f];
      if (!urlSafe) {
        out[windex] = PAD;
      }
    }
    return out;
  }

  public static void test(byte[] id, int o, int l, Writer writer) throws IOException {
    try {
      // :: error: argument
      writer.write(encodeChunk(id, o, l));
    } catch (IOException e) {
      throw e;
    }
  }

  public void test(boolean urlSafe) {
    final @RUntainted char[] base64Table = urlSafe ? BCS : BCS_URL_SAFE;
    @RUntainted char c = base64Table[0];
  }
}
