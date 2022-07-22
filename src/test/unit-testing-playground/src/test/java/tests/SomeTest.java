package tests;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class SomeTest {

    @Test
    void ok() {
        System.out.println("Done");
    }


//    @Test
//    @Disabled

    void test2() {
        System.out.println("Error");
        throw new RuntimeException("oups");
    }
}
