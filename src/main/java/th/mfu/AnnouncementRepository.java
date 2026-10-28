package th.mfu;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import th.mfu.domain.Announcement;

public interface AnnouncementRepository extends CrudRepository<Announcement, Long> {

    List<Announcement> findAllByOrderByPinnedDescCreatedAtDesc();

    List<Announcement> findTop3ByOrderByPinnedDescCreatedAtDesc();
}
