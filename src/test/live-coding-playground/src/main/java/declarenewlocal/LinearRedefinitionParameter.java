package declarenewlocal;

public class LinearRedefinitionParameter {
    public void f(int p, int j) {
        System.out.println(p);
        p = 7; //
        System.out.println(p);
        System.out.println(p);
    }
}
