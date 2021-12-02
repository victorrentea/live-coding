package playground;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class VisibleFroTestingFromTests {
    void correct() {
     new VisibleForTestingTest().methodForTestsOnly();
    }

}
