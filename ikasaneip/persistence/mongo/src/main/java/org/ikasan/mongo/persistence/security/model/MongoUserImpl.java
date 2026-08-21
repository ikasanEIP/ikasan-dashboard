package org.ikasan.mongo.persistence.security.model;
import org.ikasan.mongo.persistence.general.model.MongoConstants;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.security.model.IkasanPrincipal;
import org.ikasan.spec.security.model.Policy;
import org.ikasan.spec.security.model.Role;
import org.ikasan.spec.security.model.User;
import org.ikasan.spec.security.model.UserLite;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.security.core.GrantedAuthority;

import java.util.*;

/**
 * MongoDB implementation of User and UserLite.
 *
 * @author Ikasan Development Team
 */
public class MongoUserImpl implements User, UserLite {

    private String id;
    private String type;
    private String username;
    private String password;
    private String email;
    private String firstName;
    private String surname;
    private String department;
    private boolean enabled = true;
    private boolean accountNonExpired = true;
    private boolean accountNonLocked = true;
    private boolean credentialsNonExpired = true;
    private boolean requiresPasswordChange = false;
    private long previousAccessTimestamp;

    @JsonIgnore
    private List<String> principalIds = new ArrayList<>();

    @JsonIgnore
    private transient Set<IkasanPrincipal> principals = new HashSet<>();

    @Indexed
    private long createdTimestamp;

    @Indexed
    private long modifiedTimestamp;
    private long expiry;

    /**
     * Default no-argument constructor.
     */
    public MongoUserImpl() {}

    /**
     * Constructs a new MongoUserImpl with basic fields.
     *
     * @param username the username
     * @param password the password
     * @param email the email address
     * @param enabled whether the user is enabled
     */
    public MongoUserImpl(String username, String password, String email, boolean enabled) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.enabled = enabled;
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        if (principals != null) {
            for (IkasanPrincipal principal : principals) {
                if (principal.getRoles() != null) {
                    for (Role role : principal.getRoles()) {
                        if (role.getPolicies() != null) {
                            authorities.addAll(role.getPolicies());
                        }
                    }
                }
            }
        }
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    public void setAccountNonExpired(boolean accountNonExpired) {
        this.accountNonExpired = accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public void revokePolicy(Policy policy) {
        if (principals != null) {
            for (IkasanPrincipal principal : principals) {
                if (principal.getRoles() != null) {
                    for (Role role : principal.getRoles()) {
                        if (role.getPolicies() != null) {
                            role.getPolicies().remove(policy);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void addPrincipal(IkasanPrincipal principal) {
        if (this.principals == null) {
            this.principals = new HashSet<>();
        }
        this.principals.add(principal);

        if (this.principalIds == null) {
            this.principalIds = new ArrayList<>();
        }
        if (principal.getId() != null && !this.principalIds.contains(principal.getId().toString())) {
            this.principalIds.add(principal.getId().toString());
        }
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Override
    public String getSurname() {
        return surname;
    }

    @Override
    public void setSurname(String surname) {
        this.surname = surname;
    }

    @Override
    public String getDepartment() {
        return department;
    }

    @Override
    public void setDepartment(String department) {
        this.department = department;
    }

    @Override
    public Set<IkasanPrincipal> getPrincipals() {
        return principals;
    }

    @Override
    public void setPrincipals(Set<IkasanPrincipal> principals) {
        this.principals = principals;

        // Update principal IDs list
        if (this.principalIds == null) {
            this.principalIds = new ArrayList<>();
        } else {
            this.principalIds.clear();
        }

        if (principals != null) {
            for (IkasanPrincipal principal : principals) {
                if (principal.getId() != null) {
                    this.principalIds.add(principal.getId().toString());
                }
            }
        }
    }

    @Override
    public long getPreviousAccessTimestamp() {
        return previousAccessTimestamp;
    }

    @Override
    public void setPreviousAccessTimestamp(long previousAccessTimestamp) {
        this.previousAccessTimestamp = previousAccessTimestamp;
    }

    @Override
    public boolean isRequiresPasswordChange() {
        return requiresPasswordChange;
    }

    @Override
    public void setRequiresPasswordChange(boolean requiresPasswordChange) {
        this.requiresPasswordChange = requiresPasswordChange;
    }

    public List<String> getPrincipalIds() {
        return principalIds;
    }

    public void setPrincipalIds(List<String> principalIds) {
        this.principalIds = principalIds;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    public long getExpiry() {
        return expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MongoUserImpl user = (MongoUserImpl) o;
        return enabled == user.enabled
            && accountNonExpired == user.accountNonExpired
            && accountNonLocked == user.accountNonLocked
            && credentialsNonExpired == user.credentialsNonExpired
            && requiresPasswordChange == user.requiresPasswordChange
            && previousAccessTimestamp == user.previousAccessTimestamp
            && Objects.equals(id, user.id)
            && Objects.equals(username, user.username)
            && Objects.equals(password, user.password)
            && Objects.equals(email, user.email)
            && Objects.equals(firstName, user.firstName)
            && Objects.equals(surname, user.surname)
            && Objects.equals(department, user.department)
            && Objects.equals(principals, user.principals);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, password, email, firstName, surname,
            department, enabled, accountNonExpired, accountNonLocked, credentialsNonExpired,
            requiresPasswordChange, previousAccessTimestamp, principals);
    }
}
