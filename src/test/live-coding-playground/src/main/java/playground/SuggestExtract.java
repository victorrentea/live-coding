package playground;

import java.util.List;

public class SuggestExtract {
    int met(int x, List<String> list) {

        int t = 0;
        int y = 2;
        if (x == 2 && list.size()==0  || list.get(0) == "a") {
            return 2;
        }
        int z = x + y;

        z += y;
        list.add("a");

        return z  + 2;
    }
}
