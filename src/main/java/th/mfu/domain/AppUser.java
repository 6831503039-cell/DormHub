package th.mfu.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/** A login account. Tenants are linked to their Tenant record by email. */
@Entity
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // always stored in lower case
    @Column(unique = true, nullable = false)
    private String email;

    // BCrypt hash, never the plain password
    @Column(nullable = false)
    private String password;

    private String fullName;

    @Enumerated(EnumType.STRING)
    private Role role = Role.TENANT;

    public AppUser() {
    }

    public AppUser(String email, String password, String fullName, Role role) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }
    public void setRole(Role role) {
        this.role = role;
    }
}
