package declarenewlocal;

public class OnlyUsageInChildBlock {
    public void f(int p, int j) {
        int x;
        if (p == 1) {
            int x_ = 5; //
            System.out.println(x_);
        }
    }
}
