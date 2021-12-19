package declarenewlocal;

public class Switch {
    public void f(int p, int j) {
        int x = 0;
        switch (p) {
            case 1:
                x = 5;
                System.out.println(x);
                break;
            case 2:
                x = 7;
                System.out.println(x);
                break;
            case 3:
            case 4:
                x = 9;
                System.out.println(x);
                break;
            case 7:
                System.out.println(x);
                break;
        }
    }
}
