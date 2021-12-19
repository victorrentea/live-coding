package declarenewlocal;

public class LinearRedefinitionDerived {
    public void f(int p, int j) {
        int x = 2;
        System.out.println(x);
        int x_ = x + 7; //
        System.out.println(x_);
        System.out.println(x_);
    }
}
