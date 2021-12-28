package declarenewlocal;

public class LinearRedefinitionWithoutInitialReadBis {
    public void f(int p, int j) {
        int x = 2;
        x = 7;
        x = 9;
        System.out.println(x);
    }
}
