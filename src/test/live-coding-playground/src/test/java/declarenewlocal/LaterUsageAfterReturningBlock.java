package declarenewlocal;

public class LaterUsageAfterReturningBlock {
    public void f(int p, int j) {
        int x = 2;
        if (p == 1) {
            int x_ = 5; //
            System.out.println(x_);
            return;
        }
        System.out.println(x);
    }
}
