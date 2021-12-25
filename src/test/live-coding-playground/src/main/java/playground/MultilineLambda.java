package playground;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MultilineLambda {

    public void met(List<Integer> list) {
        list.forEach(e -> {
            System.out.println(e + e);
            System.out.println(e);
        });
        list.forEach(e ->
            System.out.println(e)
        );
    }
}
