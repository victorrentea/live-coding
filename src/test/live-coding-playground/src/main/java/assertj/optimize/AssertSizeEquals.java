package assertj.optimize;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class AssertSizeEquals {
    @Test
    public void test() {
        List<String> list = met().get();
        Assertions.assertThat(list.size()).isEqualTo(1  +1 ); //
        Assertions.assertThat(list.size()).isEqualTo(1); //
        assertThat(met().get().size()).isEqualTo(1); //
        assertThat(list.size()).isEqualTo(1); //
        assertThat(list.size()).isEqualTo(1).isEqualTo(1); //
    }

    private Supplier<List<String>> met() {
        return () -> List.of("a", "b");
    }

}
