package assertj;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AssertNotNull {
    @Test
    public void test() {
        Object actual = "x";
        Assertions.assertNotNull(actual); //
    }
}
