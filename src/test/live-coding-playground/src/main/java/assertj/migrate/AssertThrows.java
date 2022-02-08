package assertj.migrate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AssertThrows {
    @Test
    public void test() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> f()); //
        Assertions.assertThrows(IllegalArgumentException.class, () -> f(), "text");
    }

    void f() {

    }
}
