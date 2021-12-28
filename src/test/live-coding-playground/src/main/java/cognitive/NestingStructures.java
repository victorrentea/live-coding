package cognitive;

public class NestingStructures {
    public int met1() { // 0
        return 1;
    }

    public int oneIf() { // 1
        if (true) {
            return 1;
        }
        return 0;
    }

    public int twoIfs() { // 2
        if (true) {
            return 1;
        }
        if (true) {
            return 1;
        }
        return 0;
    }

    public int ifInIf() { // 3
        if (true) {
            if (true) {
                return 1;
            }
        }
        return 0;
    }
}
