class Test {

  void run() {
    Map<String, String> m;

    Map<String, @RUntainted String> a = m;
    Map<@RUntainted String, String> b = m;
    @RUntainted Map<String, String> b = m;

    // @RUntainted Map<@RUntainted String, @RUntainted String> m;
  }
}
