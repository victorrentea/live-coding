package lombok.replacerac;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ReplaceRAC_TwoArg_Service {
    private final int dummy;
    private final int dummy2;

}
