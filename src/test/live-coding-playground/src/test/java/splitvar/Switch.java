package splitvar;

public class Switch {
    public void f(int p, int j) {

        switch (p) {
            case 1: {
                int x = 5;
                System.out.println(x);
                break;
            }
            case 2: {
                int x = 7; //
                System.out.println(x);
                break;
            }
            case 3:
            case 4: {
                int x = 9; //
                System.out.println(x);
                break;
            }
            case 7: {
                int x = 0;
                System.out.println(x);
                break;
            }
        }
    }
}
