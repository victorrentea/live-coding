package declarenewlocal;

public class LaterUsageAfterThrowBlock {
    public void f(int p, int j) {
        int x = 2;
        if (p == 1) {
            x = 5; //
            System.out.println(x);
            throw new IllegalArgumentException();
        }
        System.out.println(x);
    }
}
