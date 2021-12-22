package lombok.replacerac;

import java.util.Objects;

public class ReplaceRAC_notStraightAssign {
    private final String dummy;

    public ReplaceRAC_notStraightAssign(String dummy) {
        this.dummy = Objects.requireNonNull(dummy);
    }
}
