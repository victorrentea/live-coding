package splitvar;

public class AlternateBlocksSimpler {
    public void f(int p, int j) {
        if (p == 1) {
            int x = 2 + p;
            System.out.println(x);
        } else {
            int x = 2;
            x++;
            System.out.println(x);
        }
    }
}
