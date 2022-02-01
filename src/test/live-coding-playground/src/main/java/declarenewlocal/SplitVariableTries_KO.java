package declarenewlocal;

public class SplitVariableTries_KO {


    public void allReferencesInTheSameBlocksAsAssignments(int p, int j) {
        int x = 0;
        try {
            if (p == 2) {
                if (j == 0) {
                    x = 3;
                    System.out.println(x);
                }
            } else {
                x = 2;
                x++;
                System.out.println(x);
            }
        } catch (Exception e) {
            System.out.println(x);
        }
    }
}
