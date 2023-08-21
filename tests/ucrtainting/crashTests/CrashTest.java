package crashTests;

public class CrashTest {
    private TestEnum http1 = TestEnum.HTTP_1;
    public void classPackageNullTest() {
        if(this.getClass().equals(void.class)) {
        }
    }
}

