import edu.ucr.cs.riple.taint.ucrtainting.qual.*;

public class Child extends Foo{

    // :: error: (override.param)
    public Object inherit(@RUntainted Object paramInChild){
        return paramInChild;
    }
}
