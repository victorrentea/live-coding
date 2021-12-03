package playground;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReplaceStaticImport {
    @Test
    void aTest() {
        List<Integer> list = Stream.of(1).collect(Collectors.toList());
        A aMock = Mockito.mock(A.class);
        Mockito.when(aMock.getInt()).thenReturn(-2);
        Assertions.assertThat(1).isEqualTo(1);

        Mockito.verify(aMock).getInt();
    }
}


class A {
    public int getInt() {
        return 1;
    }
}
