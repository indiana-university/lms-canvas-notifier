package edu.iu.uits.lms.canvasnotifier.service;

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
