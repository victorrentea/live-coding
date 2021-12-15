package playground;

public class SplitVariableAlternateBlocks {


    public void allReferencesInTheSameBlocksAsAssignments(int p, int j) {
        int x;

        if (p == 1) {
            x= 2 + p;
            System.out.println(x);
        } else  if (p==2){
            if (j ==0) {
                x= 3;
                System.out.println(x);
            }
        } else {
            x = 2;
             x++;
            System.out.println(x);
        }
//        System.out.println(x); // uncomment this and it should not work anymore
    }
}
