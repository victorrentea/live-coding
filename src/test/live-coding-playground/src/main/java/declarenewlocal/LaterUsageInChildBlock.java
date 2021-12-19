package declarenewlocal;

public class LaterUsageInChildBlock {
    public void f(int p, int j) {
        int x = 2;
        System.out.println(x);
        x = 6; //
        if (p == 1) {
            System.out.println(x);
        }
    }
}
