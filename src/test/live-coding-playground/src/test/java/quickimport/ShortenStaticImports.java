package quickimport;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static quickimport.ShortenStaticImports.Stupid.verify;

public class ShortenStaticImports {
    
    @Test
    void aTest() throws InterruptedException, ExecutionException, TimeoutException {
        List<Integer> list = Stream.of(1).collect(Collectors.toList());
        A aMock = Mockito.mock(A.class);
        Mockito.when(aMock.getInt()).thenReturn(-2);
        Assertions.assertThat(1).isEqualTo(1);

        verify();
        Mockito.verify(aMock, Mockito.times(1)).getInt();
        Duration d = Duration.ofMillis(100);
        Future<Integer> future = CompletableFuture.completedFuture(1);
        future.get(1, TimeUnit.MILLISECONDS);

    }
    static class A {
        public int getInt() {
            return 1;
        }
    }

    static class Stupid {
        public static void verify() {

        }
    }
}


