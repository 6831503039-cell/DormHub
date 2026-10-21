package th.mfu;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import th.mfu.domain.AppUser;
import th.mfu.domain.Feedback;

public interface FeedbackRepository extends CrudRepository<Feedback, Long> {

    List<Feedback> findAllByOrderByCreatedAtDesc();

    List<Feedback> findTop5ByOrderByCreatedAtDesc();

    List<Feedback> findByAuthorOrderByCreatedAtDesc(AppUser author);

    long countByReadByOwnerFalse();
}
