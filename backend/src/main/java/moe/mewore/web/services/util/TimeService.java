package moe.mewore.web.services.util;

import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TimeService {

    public void sleep(final long time, final TimeUnit timeUnit) throws InterruptedException {
        Thread.sleep(timeUnit.toMillis(time));
    }
}
