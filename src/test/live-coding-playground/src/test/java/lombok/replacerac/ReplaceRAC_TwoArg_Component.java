package lombok.replacerac;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ReplaceRAC_TwoArg_Component {
    private final int dummy;
    private final int dummy2;

}
