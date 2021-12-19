package declarenewlocal;

public class LaterUsageAfterReturningBlockNestedKO {
    public void f(int p, int j) {
        int x = 2;
        if (p == 1) {
            if (j == 2) {
                x = 5;
                System.out.println(x);
            }
            System.out.println(x);
            return;
        }
        System.out.println(x);
    }
}
