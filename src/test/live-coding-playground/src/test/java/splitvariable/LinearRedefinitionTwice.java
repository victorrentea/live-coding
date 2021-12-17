package splitvariable;

public class LinearRedefinitionTwice {
    public void f(int p, int j) {
        int x = 2;
        System.out.println(x);
        int x_ = 5; //
        System.out.println(x_);
        x_ = 7; //
        System.out.println(x_);
    }
}
