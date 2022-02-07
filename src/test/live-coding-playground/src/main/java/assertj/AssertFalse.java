package assertj;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AssertFalse {
    @Test
    public void test() {
        boolean actual = false;
        Assertions.assertFalse(actual); //
    }
}
