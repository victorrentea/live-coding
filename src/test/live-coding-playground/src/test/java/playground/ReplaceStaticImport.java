package playground;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReplaceStaticImport {
    
    @Test
    void aTest() throws InterruptedException, ExecutionException, TimeoutException {
        List<Integer> list = Stream.of(1).collect(Collectors.toList());
        A aMock = Mockito.mock(A.class);
        Mockito.when(aMock.getInt()).thenReturn(-2);
        Assertions.assertThat(1).isEqualTo(1);

        Mockito.verify(aMock).getInt();
        Duration d = Duration.ofMillis(100);
        Future<Integer> future = CompletableFuture.completedFuture(1);
        future.get(1, TimeUnit.MILLISECONDS);

    }
}


class A {
    public int getInt() {
        return 1;
    }
}
