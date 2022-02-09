package assertj.optimize;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AssertSizeEquals {
    @Test
    public void testList() {
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
    private Supplier<Set<String>> metSet() {
        return () -> Set.of("a", "b");
    }

    @Test
    public void testSet() {
        Set<String> set = metSet().get();
        Assertions.assertThat(set.size()).isEqualTo(1  +1 ); //
        Assertions.assertThat(set.size()).isEqualTo(0); //
        assertThat(met().get().size()).isEqualTo(1); //
        assertThat(set.size()).isEqualTo(1); //
        assertThat(set.size()).isEqualTo(1).isEqualTo(1); //
    }
    @Test
    public void testStream() {
        Stream<Integer> s = Stream.of(1, 2);
        Assertions.assertThat(s.filter(x->true).count()).isEqualTo(1); //
        Assertions.assertThat(s.count()).isEqualTo(0); //
    }
    @Test
    public void strings() {
        String s = "a";
        Assertions.assertThat(s.length()).isEqualTo(1); //
        Assertions.assertThat(s.length()).isEqualTo(0); //
    }

}
