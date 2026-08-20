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
package org.ikasan.mongo.persistence.security.model;
import org.ikasan.mongo.persistence.general.model.MongoConstants;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.ikasan.spec.security.model.Policy;
import org.ikasan.spec.security.model.Role;
import org.ikasan.spec.security.model.RoleJobPlan;
import org.ikasan.spec.security.model.RoleModule;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.*;

/**
 * MongoDB implementation of Role.
 *
 * @author Ikasan Development Team
 */
@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoRoleImpl implements Role {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("name")
    private String name;

    @Field("description")
    private String description;

    @Indexed
    @Field("created_date_time")
    private Date createdDateTime;

    @Indexed
    @Field("updated_date_time")
    private Date updatedDateTime;

    @JsonIgnore
    @Field("policy_ids")
    private List<String> policyIds = new ArrayList<>();

    @JsonIgnore
    @Field("role_module_ids")
    private List<String> roleModuleIds = new ArrayList<>();

    @JsonIgnore
    @Field("role_job_plan_ids")
    private List<String> roleJobPlanIds = new ArrayList<>();

    @JsonIgnore
    private transient Set<Policy> policies = new HashSet<>();

    @JsonIgnore
    private transient Set<RoleModule> roleModules = new HashSet<>();

    @JsonIgnore
    private transient Set<RoleJobPlan> roleJobPlans = new HashSet<>();

    /**
     * Default no-argument constructor.
     */
    public MongoRoleImpl() {}

    @Override
    public void addPolicy(Policy policy) {
        if (this.policies == null) {
            this.policies = new HashSet<>();
        }
        this.policies.add(policy);

        if (this.policyIds == null) {
            this.policyIds = new ArrayList<>();
        }
        if (policy.getId() != null && !this.policyIds.contains(policy.getId().toString())) {
            this.policyIds.add(policy.getId().toString());
        }
    }

    @Override
    public void addRoleModule(RoleModule roleModule) {
        if (this.roleModules == null) {
            this.roleModules = new HashSet<>();
        }
        this.roleModules.add(roleModule);

        if (this.roleModuleIds == null) {
            this.roleModuleIds = new ArrayList<>();
        }
        if (roleModule.getId() != null && !this.roleModuleIds.contains(roleModule.getId().toString())) {
            this.roleModuleIds.add(roleModule.getId().toString());
        }
    }

    @Override
    public void addRoleJobPlan(RoleJobPlan roleJobPlan) {
        if (this.roleJobPlans == null) {
            this.roleJobPlans = new HashSet<>();
        }
        this.roleJobPlans.add(roleJobPlan);

        if (this.roleJobPlanIds == null) {
            this.roleJobPlanIds = new ArrayList<>();
        }
        if (roleJobPlan.getId() != null && !this.roleJobPlanIds.contains(roleJobPlan.getId().toString())) {
            this.roleJobPlanIds.add(roleJobPlan.getId().toString());
        }
    }

    @Override
    public Object getId() {
        return id;
    }

    @Override
    public void setId(Object id) {
        if (id instanceof String) {
            this.id = (String) id;
        } else if (id != null) {
            this.id = id.toString();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    @Override
    public void setCreatedDateTime(Date createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    @Override
    public Date getUpdatedDateTime() {
        return updatedDateTime;
    }

    @Override
    public void setUpdatedDateTime(Date updatedDateTime) {
        this.updatedDateTime = updatedDateTime;
    }

    @Override
    public Set<Policy> getPolicies() {
        return policies;
    }

    @Override
    public void setPolicies(Set<Policy> policies) {
        this.policies = policies;

        // Update policy IDs list
        if (this.policyIds == null) {
            this.policyIds = new ArrayList<>();
        } else {
            this.policyIds.clear();
        }

        if (policies != null) {
            for (Policy policy : policies) {
                if (policy.getId() != null) {
                    this.policyIds.add(policy.getId().toString());
                }
            }
        }
    }

    @Override
    public Set<RoleModule> getRoleModules() {
        return roleModules;
    }

    @Override
    public void setRoleModules(Set<RoleModule> roleModules) {
        this.roleModules = roleModules;

        // Update role module IDs list
        if (this.roleModuleIds == null) {
            this.roleModuleIds = new ArrayList<>();
        } else {
            this.roleModuleIds.clear();
        }

        if (roleModules != null) {
            for (RoleModule roleModule : roleModules) {
                if (roleModule.getId() != null) {
                    this.roleModuleIds.add(roleModule.getId().toString());
                }
            }
        }
    }

    @Override
    public Set<RoleJobPlan> getRoleJobPlans() {
        return roleJobPlans;
    }

    @Override
    public void setRoleJobPlans(Set<RoleJobPlan> roleJobPlans) {
        this.roleJobPlans = roleJobPlans;

        // Update role job plan IDs list
        if (this.roleJobPlanIds == null) {
            this.roleJobPlanIds = new ArrayList<>();
        } else {
            this.roleJobPlanIds.clear();
        }

        if (roleJobPlans != null) {
            for (RoleJobPlan roleJobPlan : roleJobPlans) {
                if (roleJobPlan.getId() != null) {
                    this.roleJobPlanIds.add(roleJobPlan.getId().toString());
                }
            }
        }
    }

    public List<String> getPolicyIds() {
        return policyIds;
    }

    public void setPolicyIds(List<String> policyIds) {
        this.policyIds = policyIds;
    }

    public List<String> getRoleModuleIds() {
        return roleModuleIds;
    }

    public void setRoleModuleIds(List<String> roleModuleIds) {
        this.roleModuleIds = roleModuleIds;
    }

    public List<String> getRoleJobPlanIds() {
        return roleJobPlanIds;
    }

    public void setRoleJobPlanIds(List<String> roleJobPlanIds) {
        this.roleJobPlanIds = roleJobPlanIds;
    }

    @Override
    public int compareTo(Role role) {
        if (this.name == null && role.getName() == null) {
            return 0;
        }
        if (this.name == null) {
            return -1;
        }
        if (role.getName() == null) {
            return 1;
        }
        return this.name.compareTo(role.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MongoRoleImpl mongoRole = (MongoRoleImpl) o;
        return Objects.equals(id, mongoRole.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "MongoRoleImpl{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            '}';
    }
}
