package declarenewlocal;

public class LaterUsageInParentChildBlock {
    public void f(int p, int j) {
        int x = 2;
        System.out.println(x);
        if (p == 1) {
            x = 5;
            System.out.println(x);
        }
        if (p == 1) {
            System.out.println(x);
        }
    }
}
