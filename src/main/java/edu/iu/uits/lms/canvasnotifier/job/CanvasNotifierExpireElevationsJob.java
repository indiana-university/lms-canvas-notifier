package edu.iu.uits.lms.canvasnotifier.job;

import edu.iu.uits.lms.common.batch.BatchJob;
import iuonly.client.generated.api.ErrorContactApi;
import iuonly.client.generated.model.ErrorContactPostForm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@Profile("canvasnotifierexpireelevations")
public class CanvasNotifierExpireElevationsJob implements BatchJob {

    private CanvasNotifierExpireElevationsService canvasNotifierExpireElevationService;
    private ConfigurableApplicationContext ctx;

    @Autowired
    private CanvasNotifierExpireElevationsJob job;

    @Autowired
    private ErrorContactApi errorContactApi;

    public CanvasNotifierExpireElevationsJob(CanvasNotifierExpireElevationsService canvasNotifierExpireElevationService, ConfigurableApplicationContext ctx) {
        this.canvasNotifierExpireElevationService = canvasNotifierExpireElevationService;
        this.ctx = ctx;
    }

    private void expireElevations() throws IOException {
        log.info("CanvasNotifierExpireElevations job running!");
        canvasNotifierExpireElevationService.main();
    }

    @Override
    public void run() {

        try {
            job.expireElevations();
        } catch (Exception e) {
            log.error("Caught exception performing CanvasNotifierExpireElevations job", e);

            ErrorContactPostForm errorContactPostForm = new ErrorContactPostForm();
            errorContactPostForm.setJobCode(getJobCode());
            errorContactPostForm.setMessage("The Canvas Notifier Expire Elevations job has unexpectedly failed");

            errorContactApi.postEvent(errorContactPostForm);
        }

        ctx.close();
    }

    public String getJobCode() {
        return "CanvasNotifierExpireElevationsJob";
    }
}
