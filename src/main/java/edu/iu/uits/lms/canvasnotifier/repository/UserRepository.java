package edu.iu.uits.lms.canvasnotifier.repository;

import edu.iu.uits.lms.canvasnotifier.model.User;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface UserRepository extends PagingAndSortingRepository<User, Long> {

   User findByUsername(@Param("username") String username);
   User findByCanvasUserId(@Param("canvasUserId") String canvasUserId);
   List<User> findAllAuthorizedSenders();
   List<User> findAllAuthorizedUsers();

}
