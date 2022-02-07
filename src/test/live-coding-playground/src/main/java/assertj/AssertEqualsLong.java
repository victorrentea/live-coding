package assertj;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AssertEqualsLong {
    @Test
    public void test() {
        long actual = 1L;
        Assertions.assertEquals(1, actual); //
    }
}
