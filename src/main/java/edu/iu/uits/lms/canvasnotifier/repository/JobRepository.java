package edu.iu.uits.lms.canvasnotifier.repository;

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

import edu.iu.uits.lms.canvasnotifier.model.Job;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface JobRepository extends PagingAndSortingRepository<Job, Long>, ListCrudRepository<Job, Long> {

    @Query("from Job where status in ('STARTED', 'RESTARTED') order by id")
    List<Job> findAllRunningJobs();

    // 300 = 5 minutes in below query
    @Query("from Job job where job.senderWasElevated = true and job.senderIsElevated = true and EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) - 300 > EXTRACT(EPOCH FROM job.modifiedOn) order by job.id asc")
    List<Job> getElevatedJobsOlderThan();

    @Query("from Job job where job.sender_canvasid = :senderId and ((status = 'PENDING' and EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) - 300 < EXTRACT(EPOCH FROM job.modifiedOn)) or status = 'STARTED' or status = 'RESTARTED') order by job.id asc")
    List<Job> getRunningJobsBySenderCanvasId(@Param("senderId") String senderId);
}