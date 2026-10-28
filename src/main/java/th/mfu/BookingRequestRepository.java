package th.mfu;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import th.mfu.domain.AppUser;
import th.mfu.domain.BookingRequest;
import th.mfu.domain.BookingStatus;

public interface BookingRequestRepository extends CrudRepository<BookingRequest, Long> {

    List<BookingRequest> findAllByOrderByCreatedAtDesc();

    List<BookingRequest> findByStatus(BookingStatus status);

    List<BookingRequest> findByApplicantOrderByCreatedAtDesc(AppUser applicant);

    long countByStatus(BookingStatus status);
}
