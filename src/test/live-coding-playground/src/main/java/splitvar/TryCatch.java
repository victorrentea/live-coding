package splitvar;

public class TryCatch {
    public void f(int p, int j) {
        int x = 0;
        try {
            x = f();
            System.out.println(x);
        } catch (Exception e) {
            System.out.println(x);
        }
    }
    public int f() {
        throw new IllegalArgumentException();
    }
}
