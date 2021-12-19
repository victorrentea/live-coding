package declarenewlocal;

public class LinearRedefinitionDerivedPlusEqual {
    public void f(int p, int j) {
        int x = 2;
        System.out.println(x);
        int x_ = x + 2; //
        System.out.println(x_);
    }
}
