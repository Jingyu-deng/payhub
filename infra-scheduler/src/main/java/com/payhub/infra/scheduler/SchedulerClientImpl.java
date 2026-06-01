package com.payhub.infra.scheduler;

import static com.payhub.infra.scheduler.ControlJob.KEY_CONTROL_TYPE;
import static com.payhub.infra.scheduler.ControlJob.KEY_MAX_DURATION_MS;
import static com.payhub.infra.scheduler.ControlJob.KEY_REQUEST_JSON;
import static com.payhub.infra.scheduler.ControlJob.KEY_REQUEST_TYPE;
import static com.payhub.infra.scheduler.ControlJob.KEY_SCHEDULED_AT_MS;

import com.payhub.core.controls.base.Control;
import com.payhub.core.exception.JobSchedulingException;
import com.payhub.core.infra.SchedulerClient;
import com.payhub.core.utils.JsonUtils;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SchedulerClientImpl implements SchedulerClient {

  private static final Logger log = LoggerFactory.getLogger(SchedulerClientImpl.class);

  private final Scheduler quartz;

  public SchedulerClientImpl(Scheduler quartz) {
    this.quartz = quartz;
  }

  @Override
  public <I> String scheduleRecurring(
      Class<? extends Control<I, ?>> controlType,
      I request,
      Duration interval,
      Duration maxDuration) {
    long now = System.currentTimeMillis();
    String jobId = UUID.randomUUID().toString();
    JobDetail job = buildJob(jobId, controlType, request, now, maxDuration.toMillis());

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .forJob(job)
            .withIdentity(jobId)
            .startAt(new Date(now + interval.toMillis()))
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInMilliseconds(interval.toMillis())
                    .repeatForever())
            .build();

    try {
      quartz.scheduleJob(job, trigger);
      log.info(
          "Scheduled recurring job {}: {} every {}ms for {}ms",
          jobId,
          controlType.getSimpleName(),
          interval.toMillis(),
          maxDuration.toMillis());
      return jobId;
    } catch (SchedulerException e) {
      throw new JobSchedulingException("Failed to schedule recurring job: " + jobId, e);
    }
  }

  @Override
  public <I> String scheduleOnce(
      Class<? extends Control<I, ?>> controlType, I request, Duration delay) {
    String jobId = UUID.randomUUID().toString();
    JobDetail job = buildJob(jobId, controlType, request, 0, 0);

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .forJob(job)
            .withIdentity(jobId)
            .startAt(new Date(System.currentTimeMillis() + delay.toMillis()))
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withRepeatCount(0))
            .build();

    try {
      quartz.scheduleJob(job, trigger);
      log.info(
          "Scheduled one-shot job {}: {} after {}ms",
          jobId,
          controlType.getSimpleName(),
          delay.toMillis());
      return jobId;
    } catch (SchedulerException e) {
      throw new JobSchedulingException("Failed to schedule one-shot job: " + jobId, e);
    }
  }

  @Override
  public void cancel(String jobKey) {
    try {
      boolean deleted = quartz.deleteJob(JobKey.jobKey(jobKey));
      log.info("Cancelled job {}: deleted={}", jobKey, deleted);
    } catch (SchedulerException e) {
      throw new JobSchedulingException("Failed to cancel job: " + jobKey, e);
    }
  }

  private <I> JobDetail buildJob(
      String jobId,
      Class<? extends Control<I, ?>> controlType,
      I request,
      long scheduledAtMs,
      long maxDurationMs) {
    JobDataMap data = new JobDataMap();
    data.put(KEY_CONTROL_TYPE, controlType.getName());
    data.put(KEY_REQUEST_TYPE, request.getClass().getName());
    data.put(KEY_REQUEST_JSON, JsonUtils.toJson(request));
    data.put(KEY_SCHEDULED_AT_MS, scheduledAtMs);
    data.put(KEY_MAX_DURATION_MS, maxDurationMs);

    return JobBuilder.newJob(ControlJob.class)
        .withIdentity(jobId)
        .usingJobData(data)
        .storeDurably()
        .build();
  }
}
