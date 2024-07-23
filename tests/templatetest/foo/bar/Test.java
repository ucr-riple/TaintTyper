package foo.bar;

import edu.ucr.cs.riple.taint.ucrtainting.qual.*;
import java.lang.annotation.*;
import java.util.*;

public class Test {


    void test(){
        Bar b = new Bar();
        @RUntainted String s = b.getT();
    }


}


class Foo<T> {

    T getT(){
        return null;
    }
}

class Bar extends Foo<@RUntainted String>{}

class Zoo extends Bar{

    String getT() {
        return null;
    }
}

