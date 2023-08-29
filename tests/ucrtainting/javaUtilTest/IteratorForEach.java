package javaUtilTest;

import java.util.function.Consumer;
import java.util.Iterator;

class IteratorForEach<T> {
    private final Iterator<? extends T> it;
    public IteratorForEach(Iterator<? extends T> iterator) {
        this.it = iterator;
    }

    public void test(Consumer<? super T> action) {
//        it.forEachRemaining(action);
        //TODO:: turn on when checker upgraded to 3.38.0
    }
}