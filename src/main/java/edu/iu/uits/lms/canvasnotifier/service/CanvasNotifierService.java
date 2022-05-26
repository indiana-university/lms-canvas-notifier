package edu.iu.uits.lms.canvasnotifier.service;

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

import edu.iu.uits.lms.canvas.services.AccountService;
import edu.iu.uits.lms.canvas.services.CanvasService;
import edu.iu.uits.lms.canvasnotifier.model.Job;
import edu.iu.uits.lms.canvasnotifier.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class CanvasNotifierService {
    @Autowired
    private AccountService accountService;

    @Autowired
    private CanvasService canvasService;

    @Autowired
    private JobRepository jobRepository;

    /**
     * De-elevates the given Job's sender user. It will not de-elevate the user if that user
     * has any running jobs. If one sends in an already de-elevated user this method will treat
     * that as a success and return true.
     * @param job
     * @return True if the sender user was de-elevated. False if not.
     */
    public boolean deElevateSenderUser(Job job) {
        if (job == null || job.getSender_canvasid() == null || job.getSender_canvasid().trim().length() == 0) {
            return false;
        } else {
            final String senderCanvasId = job.getSender_canvasid();

            if (! accountService.isAccountAdmin(canvasService.getRootAccount(), String.valueOf(job.getSender_canvasid()))) {
                return true;
            }

            List<Job> runningJobs = jobRepository.getRunningJobsBySenderCanvasId(senderCanvasId);

            if (runningJobs == null || runningJobs.size() == 0 &&
                    accountService.revokeAsAccountAdmin(canvasService.getRootAccount(), senderCanvasId)) {
                return true;
            } else {
                log.info("Cannot de-elevate sender user {}, as they have running jobs", senderCanvasId);
            }
        }

        return false;
    }
}
