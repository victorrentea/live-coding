package quickimport;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class AddStaticImports {
    @Test
    void aTest() {
        List<Integer> list = Stream.of(1).collect(toList());
        A aMock = mock(A.class);
        when(aMock.getInt()).thenReturn(-2);
        assertThat(1).isEqualTo(1);

        verify(aMock).getInt();
    }
    static class A {
        public int getInt() {
            return 1;
        }
    }
}

