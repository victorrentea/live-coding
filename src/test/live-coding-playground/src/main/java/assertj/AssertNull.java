package assertj;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AssertNull {
    @Test
    public void test() {
        Object actual = "x";
        Assertions.assertNull(actual); //
    }
}
