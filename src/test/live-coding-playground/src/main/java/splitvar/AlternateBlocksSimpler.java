package splitvar;

public class AlternateBlocksSimpler {
    public void f(int p, int j) {
        int x; // 1
        if (p == 1) {
            x = 2 + p;
            System.out.println(x);
        } else {
            x = 2;
            x++;
            System.out.println(x);
        }
    }
}
