package org.ikasan.mongo.persistence.security.repository;

import org.ikasan.mongo.persistence.security.model.MongoUserImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for User persistence.
 *
 * @author Ikasan Development Team
 */
@Repository
@DependsOn("mongoTemplate")
public interface MongoUserRepository extends MongoRepository<MongoUserImpl, String> {

    /**
     * Find a user by username.
     *
     * @param username the username to search for
     * @return Optional containing the user if found
     */
    Optional<MongoUserImpl> findByUsername(String username);

    /**
     * Find users whose username contains the search term (case-insensitive).
     *
     * @param username the search term
     * @return list of matching users
     */
    List<MongoUserImpl> findByUsernameContainingIgnoreCase(String username);

    /**
     * Find users whose first name contains the search term (case-insensitive).
     *
     * @param firstName the search term
     * @return list of matching users
     */
    List<MongoUserImpl> findByFirstNameContainingIgnoreCase(String firstName);

    /**
     * Find users whose surname contains the search term (case-insensitive).
     *
     * @param surname the search term
     * @return list of matching users
     */
    List<MongoUserImpl> findBySurnameContainingIgnoreCase(String surname);

    /**
     * Find users associated with a specific principal.
     *
     * @param principalId the principal identifier
     * @return list of users associated with the principal
     */
    List<MongoUserImpl> findByPrincipalIdsContaining(String principalId);

    /**
     * Delete a user by username.
     *
     * @param username the username of the user to delete
     */
    void deleteByUsername(String username);

    /**
     * Delete users with expiry less than the specified timestamp.
     *
     * @param currentTime the current timestamp
     */
    void deleteByExpiryLessThan(long currentTime);
}
