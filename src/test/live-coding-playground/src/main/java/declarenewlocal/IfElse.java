package declarenewlocal;

public class IfElse {
    public void f(int p, int j) {
        int x = 0; // Split Variable !
        if (p == 1) {
            x = 5;
            System.out.println(x);
        } else {
            System.out.println(x);
        }
    }
}
