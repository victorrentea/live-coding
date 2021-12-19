package declarenewlocal;

public class LaterUsageAfterReturningBlockNestedOK {
    public void f(int p, int j) {
        int x = 2;
        if (p == 1) {
            if (j == 2) {
                int x_ = 5; //
                System.out.println(x_);
            }
            return;
        }
        System.out.println(x);
    }
}
