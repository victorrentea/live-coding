package playground;

public class SplitVariable {

    void method (boolean b) {
        int x;
        if (b) {
            x= 2;
            int y;
            y= x + 4;
            System.out.println(x);
            x= 3;
            System.out.println(x);
        } else {
            x= 3;
            System.out.println(x);
        }
    }
}
