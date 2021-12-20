package declarenewlocal;

public class SplitVariableTries {


    public void allReferencesInTheSameBlocksAsAssignments(int p, int j) {
        int x;
        try {
            if (p == 2) {
                if (j == 0) {
                    int x_ = 3;
                    System.out.println(x_);
                }
            } else {
                int x_ = 2;
                x_++;
                System.out.println(x_);
            }
        } catch (Exception e) {
            int x_ = 2;
            System.out.println(x_);
        }
    }
}
