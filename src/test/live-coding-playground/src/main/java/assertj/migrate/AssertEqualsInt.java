package assertj.migrate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AssertEqualsInt {
    @Test
    public void test() {
        int actual = 1;
        Assertions.assertEquals(1, actual); //
        Assertions.assertEquals(1, actual, "Message"); //
    }
}
