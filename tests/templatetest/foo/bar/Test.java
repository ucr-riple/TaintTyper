package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;

public class Test {

  public void test(String redirectUri) {
    // :: error: assignment
    @RUntainted String test = OpenCms.getLinkManager().substituteLink(getCmsObject(), redirectUri, null, true);
  }

  public CmsObject getCmsObject() {
    return null;
  }

  static class OpenCms {
    public static LinkManager getLinkManager() {
      return null;
    }
  }

  static class LinkManager {
      public String substituteLink(CmsObject cms, String link, String siteRoot, boolean forceSecure) {
          return substituteLink(cms, link, siteRoot, null, forceSecure);
      }

      public String substituteLink(
              CmsObject cms, String link, String siteRoot, String targetDetailPage, boolean forceSecure) {
          return null;
      }
  }

  static class CmsObject {}
}
