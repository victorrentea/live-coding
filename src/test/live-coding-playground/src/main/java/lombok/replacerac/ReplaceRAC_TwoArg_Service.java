package lombok.replacerac;

import org.springframework.stereotype.Service;

@Service
public class ReplaceRAC_TwoArg_Service {
    private final int dummy;
    private final int dummy2;

    public ReplaceRAC_TwoArg_Service(int dummy, int dummy2) { //
        this.dummy = dummy;
        this.dummy2 = dummy2;
    }
}
