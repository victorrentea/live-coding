package declarenewlocal;

public class LaterUsageAfterThrowBlock {
    public void f(int p, int j) {
        int x = 2;
        if (p == 1) {
            int x_ = 5; //
            System.out.println(x_);
            throw new IllegalArgumentException();
        }
        System.out.println(x);
    }
}
