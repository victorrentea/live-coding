package declarenewlocal;

public class LaterUsageAfterReturningBlockNestedOK {
    public void f(int p, int j) {
        int x = 2;
        if (p == 1) {
            if (j == 2) {
                x = 5; //
                System.out.println(x);
            }
            return;
        }
        System.out.println(x);
    }
}
