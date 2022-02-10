package edu.iu.uits.lms.canvasnotifier.repository;

import edu.iu.uits.lms.canvasnotifier.model.Job;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface JobRepository extends PagingAndSortingRepository<Job, Long> {
    List<Job> findAllRunningJobs();
    List<Job> getElevatedJobsOlderThan();
    List<Job> getRunningJobsBySenderCanvasId(@Param("senderId") String senderId);
}
