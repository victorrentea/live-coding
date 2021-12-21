package declarenewlocal;

public class TryCatch {
    public void f(int p, int j) {
        int x = 0;
        try {
            x = f();
            if (p == 0) {
                x = 4;
                System.out.println(x);
            }
            System.out.println(x);
        } catch (Exception e) {
            System.out.println(x);
        }
    }
    public int f() {
        throw new IllegalArgumentException();
    }
}
