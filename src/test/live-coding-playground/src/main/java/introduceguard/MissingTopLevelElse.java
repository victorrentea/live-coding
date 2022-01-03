package introduceguard;

public class MissingTopLevelElse {
    public void method(int x) {
        if (x != 0) { //
            for (int i=0;i<x;i++) {
                if (i == x%2) {
                    System.out.println("Stuff to do");
                    if (x == 5) {
                        System.out.println("More stuff to do");
                    }
                }
            }
        }
    }
}
