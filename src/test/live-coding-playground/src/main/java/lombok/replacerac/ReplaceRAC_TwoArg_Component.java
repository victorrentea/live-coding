package lombok.replacerac;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class ReplaceRAC_TwoArg_Component {
    private final int dummy;
    private final int dummy2;

    public ReplaceRAC_TwoArg_Component(int dummy, int dummy2) { //
        this.dummy = dummy;
        this.dummy2 = dummy2;
    }
}
