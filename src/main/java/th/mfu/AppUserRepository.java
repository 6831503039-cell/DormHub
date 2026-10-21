package th.mfu;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import th.mfu.domain.AppUser;

public interface AppUserRepository extends CrudRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
