package declarenewlocal;

public class LoopAssignedUsedLater {
    public void f(int p, int j) {
        int x = 2;
        System.out.println(x);
        for (int i = 0; i < j; i++) {
            x = 5;
            System.out.println(x);
        }
        System.out.println(x);
    }
}
