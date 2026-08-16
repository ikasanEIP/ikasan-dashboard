/*
 * $Id$
 * $URL$
 *
 * ====================================================================
 * Ikasan Enterprise Integration Platform
 *
 * Distributed under the Modified BSD License.
 * Copyright notice: The copyright for this software and a full listing
 * of individual contributors are as shown in the packaged copyright.txt
 * file.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  - Neither the name of the ORGANIZATION nor the names of its contributors may
 *    be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 */
package org.ikasan.mongo.persistence.security.repository;

import org.ikasan.mongo.persistence.security.model.MongoPolicyImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for Policy persistence.
 *
 * @author Ikasan Development Team
 */
@Repository
@DependsOn("mongoTemplate")
public interface MongoPolicyRepository extends MongoRepository<MongoPolicyImpl, String> {

    /**
     * Find a policy by name.
     *
     * @param name the name to search for
     * @return Optional containing the policy if found
     */
    Optional<MongoPolicyImpl> findByName(String name);

    /**
     * Find policies whose name contains the search term (case-insensitive).
     *
     * @param name the search term
     * @return list of matching policies
     */
    List<MongoPolicyImpl> findByNameContainingIgnoreCase(String name);

    /**
     * Find policies whose description contains the search term (case-insensitive).
     *
     * @param description the search term
     * @return list of matching policies
     */
    List<MongoPolicyImpl> findByDescriptionContainingIgnoreCase(String description);

    /**
     * Delete a policy by name.
     *
     * @param name the name of the policy to delete
     */
    void deleteByName(String name);

    /**
     * Check if a policy exists by name.
     *
     * @param name the name to check
     * @return true if a policy with the given name exists
     */
    boolean existsByName(String name);
}
