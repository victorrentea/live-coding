package assertj.migrate;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AssertEqualsObject {
    @Test
    public void test() {
        String actual = "a";
        Assert.assertEquals("a", actual); //
        Assertions.assertEquals("a", actual); //
        Assertions.assertNotEquals("b", actual); //
    }
}
