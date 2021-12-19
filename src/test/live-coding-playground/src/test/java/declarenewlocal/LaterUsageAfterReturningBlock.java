package declarenewlocal;

public class LaterUsageAfterReturningBlock {
    public void f(int p, int j) {
        if (p == 1) {
            int x = 5;
            System.out.println(x);
            return;
        }
        int x = 2;
        System.out.println(x);
    }
}
