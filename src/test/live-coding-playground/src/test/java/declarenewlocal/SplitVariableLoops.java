package declarenewlocal;

public class SplitVariableLoops {


    public void allReferencesInTheSameBlocksAsAssignments(int p, int j) {
        int x = 3;

        for (int i = 0; i < 2; i++) {
            x = 2 + p;
            System.out.println(x);
        }
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
    }
}
