package com.battlelab.planer;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;


/**
 * @author willsun
 */
public class TaskScheduler {
    @FunctionName("TaskScheduler")
    public static void run(
            @TimerTrigger(name = "timerInfo", schedule = "0 */1 * * * *") String timerInfo,
            final ExecutionContext context) throws InterruptedException {
        PlanHelper.plan(context.getLogger());
    }
}
