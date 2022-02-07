package assertj;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AssertThrows {
    @Test
    public void test() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> f()); //
    }

    void f() {

    }
}
