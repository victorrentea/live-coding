package declarenewlocal;

public class SplitVariableTries {


    public void allReferencesInTheSameBlocksAsAssignments(int p, int j) {
        int x;
        try {
            if (p == 2) {
                if (j == 0) {
                    int x_ = 3; //
                    System.out.println(x_);
                }
            } else {
                x = 2; //
                x++;
                System.out.println(x);
            }
        } catch (Exception e) {
            x = 2; //
            System.out.println(x);
        }
    }
}
