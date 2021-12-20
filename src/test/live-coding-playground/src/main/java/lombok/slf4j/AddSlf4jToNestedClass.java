package lombok.slf4j;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AddSlf4jToNestedClass {
//    private final Logger log = LoggerFactory.getLogger(AddSlf4jToNestedClass.class);


    static class Nested {
        void method() {
            log
        }
    }
}
