package declarenewlocal;

public class LinearRedefinitionWithoutInitialRead {
    public void f(int p, int j) {
        int x = 2;
        x = 7;
        System.out.println(x);
    }
}
