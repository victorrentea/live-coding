package assertj;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AssertTrue {
    @Test
    public void test() {
        boolean actual = true;
        Assertions.assertTrue(actual); //
    }
}
