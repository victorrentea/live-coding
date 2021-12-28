package cognitive;

public class BooleanExpressions {
    public boolean method(int a, int b) { // 1
        return a == 1 && b == 2;
    }
    public boolean method2(int a, int b) { // 3
        return a == 1 && b == 2 || a == 3 && b == 5;
    }
}
