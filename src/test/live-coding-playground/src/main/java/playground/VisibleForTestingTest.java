package playground;

import com.google.common.annotations.VisibleForTesting;

public class VisibleForTestingTest {

    @VisibleForTesting
    public void methodForTestsOnly() {

    }


    public void abuser() {
        methodForTestsOnly();
    }
}
