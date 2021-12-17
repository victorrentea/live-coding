package splitvariable;

public class LaterUsageInParentBlockAfterReassign {
    public void f(int p, int j) {
        int x = 2;
        System.out.println(x);
        if (p == 1) {
            int x_ = 5; //
            System.out.println(x_);
        }
        x = 7; //
        System.out.println(x);
    }
}
