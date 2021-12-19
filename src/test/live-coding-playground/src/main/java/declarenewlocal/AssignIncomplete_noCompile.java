package declarenewlocal;

public class AssignIncomplete_noCompile {
    public void f(int p, int j) {
        int x = 2;
        System.out.println(x);
        x /= ;
        System.out.println(x);
    }
}
