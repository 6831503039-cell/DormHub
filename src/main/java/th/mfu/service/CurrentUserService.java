package th.mfu.service;

import java.security.Principal;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import th.mfu.AppUserRepository;
import th.mfu.TenantRepository;
import th.mfu.domain.AppUser;
import th.mfu.domain.Tenant;

@Service
public class CurrentUserService {

    private final AppUserRepository users;
    private final TenantRepository tenants;

    public CurrentUserService(AppUserRepository users, TenantRepository tenants) {
        this.users = users;
        this.tenants = tenants;
    }

    public AppUser user(Principal principal) {
        return users.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    /** The tenant record whose email matches the account; empty until the admin adds it. */
    public Optional<Tenant> tenant(AppUser user) {
        return tenants.findFirstByEmailIgnoreCase(user.getEmail());
    }

    /** True when the logged-in account is a tenant with a room (the full tenant menu is shown). */
    public boolean hasRoom(Principal principal) {
        return tenants.findFirstByEmailIgnoreCase(principal.getName()).map(t -> t.getRoom() != null).orElse(false);
    }
}
