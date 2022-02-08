package assertj.migrate;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AssertEqualsDouble {
    @Test
    public void test() {
        double actual = 1d;
        Assert.assertEquals(1d, actual); //
        Assertions.assertEquals(1d, actual); //
    }
}
