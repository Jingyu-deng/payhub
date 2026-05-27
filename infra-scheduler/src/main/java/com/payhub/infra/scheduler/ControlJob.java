package com.payhub.infra.scheduler;

import com.payhub.core.controls.base.Control;
import com.payhub.core.infra.ControlClient;
import com.payhub.core.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Quartz job that deserializes a request DTO from {@link JobDataMap}, resolves the {@link Control}
 * implementation from the Spring context, and calls {@link Control#execute(Object)}.
 */
@Slf4j
@RequiredArgsConstructor
public class ControlJob extends QuartzJobBean {

  static final String KEY_CONTROL_TYPE = "controlType";
  static final String KEY_REQUEST_TYPE = "requestType";
  static final String KEY_REQUEST_JSON = "requestJson";
  static final String KEY_MAX_DURATION_MS = "maxDurationMs";
  static final String KEY_SCHEDULED_AT_MS = "scheduledAtMs";

  private final ControlClient controlClient;

  @Override
  protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
    JobDataMap data = context.getJobDetail().getJobDataMap();
    String controlTypeName = data.getString(KEY_CONTROL_TYPE);
    String requestTypeName = data.getString(KEY_REQUEST_TYPE);
    String requestJson = data.getString(KEY_REQUEST_JSON);

    long maxDurationMs = data.getLongValue(KEY_MAX_DURATION_MS);
    long scheduledAtMs = data.getLongValue(KEY_SCHEDULED_AT_MS);
    if (maxDurationMs > 0 && System.currentTimeMillis() - scheduledAtMs >= maxDurationMs) {
      log.info(
          "Recurring job {} exceeded maxDuration ({}ms), cancelling",
          context.getJobDetail().getKey(),
          maxDurationMs);
      try {
        context.getScheduler().deleteJob(context.getJobDetail().getKey());
      } catch (Exception e) {
        log.error("Failed to cancel job after exceeding maxDuration: {}", e.getMessage(), e);
      }
      return;
    }

    try {
      Object request = JsonUtils.fromJson(requestJson, Class.forName(requestTypeName));

      Class controlType = Class.forName(controlTypeName);
      Control control = controlClient.getControl(controlType);
      Object result = control.execute(request);

      if (result instanceof Boolean done && done) {
        log.info(
            "Control {} returned true, cancelling recurring job {}",
            controlTypeName,
            context.getJobDetail().getKey());
        context.getScheduler().deleteJob(context.getJobDetail().getKey());
      }
    } catch (Exception e) {
      log.error("Failed to execute scheduled control {}: {}", controlTypeName, e.getMessage(), e);
      throw new JobExecutionException(e);
    }
  }
}
