package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vmlens.api.AllInterleavings;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

public class ThreadSafeTest {

    public void update(ConcurrentHashMap<Integer, Integer> map) {

        synchronized (this) {
            Integer result = map.get(1);

            if (result == null) {
                map.put(1, 1);
            } else {
                map.put(1, result + 1);
            }
        }
    }

    @Test
    public void testUpdate() throws InterruptedException {
        try (AllInterleavings allInterleavings = new AllInterleavings("ThreadSafeTest"); ) {
            // surround the test with a while loop, iterationg over
            // the class AllInterleavings
            while (allInterleavings.hasNext()) {
                ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<Integer, Integer>();

                Thread first =
                        new Thread() {

                            @Override
                            public void run() {
                                update(map);
                            }
                        };

                Thread second =
                        new Thread() {

                            @Override
                            public void run() {
                                update(map);
                            }
                        };

                first.start();
                second.start();
                first.join();
                second.join();
                assertEquals(2, map.get(1).intValue());
            }
        }
    }
}
