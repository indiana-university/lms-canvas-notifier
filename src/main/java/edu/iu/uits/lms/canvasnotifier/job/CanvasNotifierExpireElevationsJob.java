package edu.iu.uits.lms.canvasnotifier.job;

/*-
 * #%L
 * canvasnotifier
 * %%
 * Copyright (C) 2015 - 2022 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import edu.iu.uits.lms.common.batch.BatchJob;
import edu.iu.uits.lms.iuonly.model.errorcontact.ErrorContactPostForm;
import edu.iu.uits.lms.iuonly.services.ErrorContactServiceImpl;
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

    @Autowired
    private CanvasNotifierExpireElevationsService canvasNotifierExpireElevationService;

    @Autowired
    private ConfigurableApplicationContext ctx;

    @Autowired
    private ErrorContactServiceImpl errorContactService;

    private void expireElevations() throws IOException {
        log.info("CanvasNotifierExpireElevations job running!");
        canvasNotifierExpireElevationService.main();
    }

    @Override
    public void run() {

        try {
            expireElevations();
        } catch (Exception e) {
            log.error("Caught exception performing CanvasNotifierExpireElevations job", e);

            ErrorContactPostForm errorContactPostForm = new ErrorContactPostForm();
            errorContactPostForm.setJobCode(getJobCode());
            errorContactPostForm.setMessage("The Canvas Notifier Expire Elevations job has unexpectedly failed");

            errorContactService.postEvent(errorContactPostForm);
        }

        ctx.close();
    }

    public String getJobCode() {
        return "CanvasNotifierExpireElevationsJob";
    }
}
