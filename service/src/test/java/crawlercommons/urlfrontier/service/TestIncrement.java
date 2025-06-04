package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vmlens.api.AllInterleavings;
import org.junit.jupiter.api.Test;

public class TestIncrement {

    private int j = 0;

    @Test
    public void testIncrement() throws InterruptedException {
        try (AllInterleavings allInterleavings = new AllInterleavings("tutorial")) {
            while (allInterleavings.hasNext()) {
                j = 0;
                Thread first =
                        new Thread() {
                            @Override
                            public void run() {
                                j++;
                            }
                        };
                first.start();
                j++;
                first.join();
                assertEquals(2, j);
            }
        }
    }
}
