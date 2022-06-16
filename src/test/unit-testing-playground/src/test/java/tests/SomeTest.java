package tests;

import org.junit.jupiter.api.Test;

public class SomeTest {

    @Test
    void test() {
        System.out.println("Done");
        throw new RuntimeException("oups");
    }
}
