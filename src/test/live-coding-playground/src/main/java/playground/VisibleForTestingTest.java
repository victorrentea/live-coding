package playground;

import com.google.common.annotations.VisibleForTesting;

public class VisibleForTestingTest {

    @VisibleForTesting
    public void methodForTestsOnly() {

    }


    public void abuser() {
        methodForTestsOnly();
    }

    public static class Loop {
        public void f(int p, int j) {
            int x = 2;
            System.out.println(x);

            for (int i = 0; i < 100; i++) {
                x = x + 2;
            }

        }
    }
}
