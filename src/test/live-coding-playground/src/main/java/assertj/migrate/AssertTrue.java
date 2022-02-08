package assertj.migrate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AssertTrue {
    @Test
    public void test() {
        boolean actual = true;
        Assertions.assertTrue(actual); //
    }
}
