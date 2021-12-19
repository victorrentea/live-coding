package declarenewlocal;

public class LoopAssignedNotUsedLater {
    public void f(int p, int j) {
        int x = 2;
        System.out.println(x);
        for (int i = 0; i < j; i++) {
            int x_ = 5; //
            System.out.println(x_);
        }
    }
}
