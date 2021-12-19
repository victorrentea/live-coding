package declarenewlocal;

public class LaterUsageAfterReturningBlockKO {
    public void f(int p, int j) {
        int x = 2;
        if (p == 1) {
            x = 5;
            System.out.println(x);
            if (j==0) return;
        }
        System.out.println(x);
    }
}
