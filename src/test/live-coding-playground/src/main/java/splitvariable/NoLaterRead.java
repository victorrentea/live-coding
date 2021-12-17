package splitvariable;

public class NoLaterRead {
    public void f(int p, int j) {
        int x = 2;
        System.out.println(x);

        x = 7;

    }
}
