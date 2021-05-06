package edu.iu.uits.lms.canvasnotifier.repository;

import edu.iu.uits.lms.canvasnotifier.model.Recipient;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface RecipientRepository extends PagingAndSortingRepository<Recipient, Long> {
    Recipient findProcessedForJobByUsername(@Param("jobId") Long jobId, @Param("username") String username);
    List<Recipient> findByJob(@Param("jobId") Long jobId);
}
