package declarenewlocal;

public class LinearRedefinitionDerived {
    public void f(int p, int j) {
        int x = 2;
        System.out.println(x);
        x = x + 7; //
        System.out.println(x);
        System.out.println(x);
    }
}
