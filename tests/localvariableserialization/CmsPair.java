package test;

public class CmsPair<A, B> {

    private A m_first;

    private B m_second;

    public CmsPair(A a, B b) {
        m_first = a;
        m_second = b;
    }
    public static <A, B> CmsPair<A, B> create(A a, B b) {
        return new CmsPair<A, B>(a, b);
    }
}