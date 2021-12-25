package playground;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestBase {
    @BeforeEach
    void before() {

    }
}

public class BeforeOverride extends TestBase{
    @BeforeEach
    void before() {

    }
    @Test
    void test() {

    }
}
