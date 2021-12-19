package declarenewlocal;

public class IfElseDeeper {
    public void f(int p, int j) {
        int x = 0;
        if (p == 1) {
            if (j == 2) {
                x = 5; //
                System.out.println(x);
            }
        } else
            System.out.println(x);
    }
}
