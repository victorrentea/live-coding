package declarenewlocal;

public class LaterUsageAfterReturningBlock {
    public void f(int p, int j) {
        int x = 2;
        if (p == 1) {
            x = 5; //
            System.out.println(x);
            return;
        }
        System.out.println(x);
    }
}
