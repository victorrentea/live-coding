package splitvariable;

public class LaterUsageInParentBlock {
    public void f(int p, int j) {
        int x = 2;
        System.out.println(x);
        if (p == 1) {
            x = 5;
            System.out.println(x);
        }
        int x_ = 7;
        System.out.println(x_);
    }
}
