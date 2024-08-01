package springSecOAuth;

import edu.xxx.cs.yyyyy.taint.tainttyper.qual.RUntainted;
import javax.servlet.http.HttpSession;

public class SpringTests {

  public void storeToken(HttpSession session, @RUntainted String expiresInValue) {

    // adding support for oauth session extension
    // (https://oauth.googlecode.com/svn/spec/ext/session/1.0/drafts/1/spec.html)
    Long expiration = null;
    if (expiresInValue != null) {
      try {
        expiration = System.currentTimeMillis() + (Integer.parseInt(expiresInValue) * 1000);
      } catch (NumberFormatException e) {
        // fall through.
      }
    }

    if (expiration != null) {
      session.setAttribute("#EXPIRATION", expiration);
    }
  }
}
