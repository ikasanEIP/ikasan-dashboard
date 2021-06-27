package org.ikasan.dashboard.ui.scheduler;

import org.junit.Test;
import org.quartz.CronExpression;

import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

public class CronExpressionTest {

    @Test
    public void test() throws ParseException {
        CronExpression exp = new CronExpression("0 15 10 * * ? *");
        AtomicReference<Date> next = new AtomicReference<>();

        IntStream.range(0, 10).forEach( i -> {
            if(next.get() == null) {
                next.set(exp.getNextValidTimeAfter(new Date()));

            }
            else {
                next.set(exp.getNextValidTimeAfter(next.get()));
            }

            System.out.println(next.get());
        });
    }
}
