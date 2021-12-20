package declarenewlocal;

public class LinearRedefinitionParameter {
    public void f(int p, int j) {
        System.out.println(p);
        int p_ = 7; //
        System.out.println(p_);
        System.out.println(p_);
    }
}
