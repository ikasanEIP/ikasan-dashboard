package org.ikasan.security.initialisation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.security.dao.SolrIkasanPrincipalDaoImpl;
import org.ikasan.security.dao.SolrPolicyDaoImpl;
import org.ikasan.security.dao.SolrRoleDaoImpl;
import org.ikasan.security.dao.SolrUserDaoImpl;
import org.ikasan.security.model.SolrIkasanPrincipalImpl;
import org.ikasan.security.model.SolrPolicyImpl;
import org.ikasan.security.model.SolrRoleImpl;
import org.ikasan.security.model.SolrUserImpl;
import org.ikasan.spec.security.model.IkasanPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class BaselineSecurityDataLoader {
    private static final Logger logger = LoggerFactory.getLogger(BaselineSecurityDataLoader.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SolrPolicyDaoImpl policyDao;
    private final SolrRoleDaoImpl roleDao;
    private final SolrIkasanPrincipalDaoImpl principalDao;
    private final SolrUserDaoImpl userDao;

    public BaselineSecurityDataLoader(SolrPolicyDaoImpl policyDao,
                                     SolrRoleDaoImpl roleDao,
                                     SolrIkasanPrincipalDaoImpl principalDao,
                                     SolrUserDaoImpl userDao) {
        this.policyDao = policyDao;
        this.roleDao = roleDao;
        this.principalDao = principalDao;
        this.userDao = userDao;
    }

    public void loadBaselineData() throws IOException {
        logger.info("Loading baseline security data into Solr...");

        // Load data from JSON files
        List<PolicyData> policyDataList = loadPolicies();
        List<RoleData> roleDataList = loadRoles();
        List<PrincipalData> principalDataList = loadPrincipals();
        List<UserData> userDataList = loadUsers();

        logger.info("Loaded {} policies, {} roles, {} principals, {} users from JSON files",
            policyDataList.size(), roleDataList.size(), principalDataList.size(), userDataList.size());

        // Create policy map for lookups
        Map<String, SolrPolicyImpl> policyMap = new HashMap<>();

        // Save policies to Solr
        logger.info("Writing {} policies to Solr...", policyDataList.size());
        for (PolicyData policyData : policyDataList) {
            SolrPolicyImpl policy = new SolrPolicyImpl();
            policy.setName(policyData.getName());
            policy.setDescription(policyData.getDescription());
            policy.setCreatedDateTime(new Date());
            policy.setUpdatedDateTime(new Date());

            policyDao.saveOrUpdatePolicy(policy);
            policyMap.put(policy.getName(), policy);
            logger.debug("Saved policy: {}", policy.getName());
        }

        // Save roles to Solr with their policies
        logger.info("Writing {} roles to Solr...", roleDataList.size());
        Map<String, SolrRoleImpl> roleMap = new HashMap<>();
        for (RoleData roleData : roleDataList) {
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName(roleData.getName());
            role.setDescription(roleData.getDescription());
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            // Add policies to role
            if (roleData.getPolicyNames() != null) {
                for (String policyName : roleData.getPolicyNames()) {
                    SolrPolicyImpl policy = policyMap.get(policyName);
                    if (policy != null) {
                        role.addPolicy(policy);
                    } else {
                        logger.warn("Policy not found: {} for role: {}", policyName, role.getName());
                    }
                }
            }

            roleDao.saveOrUpdateRole(role);
            roleMap.put(role.getName(), role);
            logger.debug("Saved role: {} with {} policies", role.getName(),
                roleData.getPolicyNames() != null ? roleData.getPolicyNames().size() : 0);
        }

        // Update policies with their related role identifiers (bidirectional relationship)
        logger.info("Updating policies with related role identifiers...");
        for (PolicyData policyData : policyDataList) {
            if (policyData.getRoleNames() != null && !policyData.getRoleNames().isEmpty()) {
                SolrPolicyImpl policy = policyMap.get(policyData.getName());
                if (policy != null) {
                    for (String roleName : policyData.getRoleNames()) {
                        SolrRoleImpl role = roleMap.get(roleName);
                        if (role != null) {
//                            policy.(role);
                        } else {
                            logger.warn("Role not found: {} for policy: {}", roleName, policy.getName());
                        }
                    }
                    policyDao.saveOrUpdatePolicy(policy);
                    logger.debug("Updated policy: {} with {} roles", policy.getName(), policyData.getRoleNames().size());
                } else {
                    logger.warn("Policy not found in map: {}", policyData.getName());
                }
            }
        }

        // Save principals to Solr with their roles
        logger.info("Writing {} principals to Solr...", principalDataList.size());
        Map<String, SolrIkasanPrincipalImpl> principalMap = new HashMap<>();
        for (PrincipalData principalData : principalDataList) {
            SolrIkasanPrincipalImpl principal = new SolrIkasanPrincipalImpl();
            principal.setName(principalData.getName());
            principal.setType(principalData.getPrincipalType());
            principal.setDescription(principalData.getDescription());
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());

            // Add roles to principal
            if (principalData.getRoleNames() != null) {
                for (String roleName : principalData.getRoleNames()) {
                    SolrRoleImpl role = roleMap.get(roleName);
                    if (role != null) {
                        principal.addRole(role);
                    } else {
                        logger.warn("Role not found: {} for principal: {}", roleName, principal.getName());
                    }
                }
            }

            principalDao.saveOrUpdatePrincipal(principal);
            principalMap.put(principal.getName(), principal);
            logger.debug("Saved principal: {} with {} roles", principal.getName(),
                principalData.getRoleNames() != null ? principalData.getRoleNames().size() : 0);
        }

        // Save users to Solr with their principals
        logger.info("Writing {} users to Solr...", userDataList.size());
        for (UserData userData : userDataList) {
            SolrUserImpl user = new SolrUserImpl();
            user.setUsername(userData.getUsername());
            user.setPassword(userData.getPassword());
            user.setEmail(userData.getEmail());
            user.setFirstName(userData.getFirstName());
            user.setSurname(userData.getSurname());
            user.setDepartment(userData.getDepartment());
            user.setEnabled(userData.isEnabled());

            // Add principals to user
            if (userData.getPrincipalNames() != null) {
                Set<IkasanPrincipal> principals = new HashSet<>();
                for (String principalName : userData.getPrincipalNames()) {
                    SolrIkasanPrincipalImpl principal = principalMap.get(principalName);
                    if (principal != null) {
                        principals.add(principal);
                    } else {
                        logger.warn("Principal not found: {} for user: {}", principalName, user.getUsername());
                    }
                }
                user.setPrincipals(principals);
            }

            userDao.save(user);
            logger.debug("Saved user: {} with {} principals", user.getUsername(),
                userData.getPrincipalNames() != null ? userData.getPrincipalNames().size() : 0);
        }

        logger.info("Baseline security data successfully loaded into Solr!");
    }

    private List<PolicyData> loadPolicies() throws IOException {
        logger.info("Loading policies from baseline-security/policies.json");
        List<PolicyData> policies = new ArrayList<>();

        try (InputStream is = new ClassPathResource("baseline-security/policies.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            for (JsonNode node : root) {
                PolicyData policy = new PolicyData();
                policy.setName(node.get("name").asText());
                policy.setDescription(node.get("description").asText());

                List<String> roleNames = new ArrayList<>();
                JsonNode rolesNode = node.get("roles");
                if (rolesNode != null && rolesNode.isArray()) {
                    for (JsonNode roleNode : rolesNode) {
                        roleNames.add(roleNode.asText());
                    }
                }
                policy.setRoleNames(roleNames);

                policies.add(policy);
            }
        }

        logger.info("Loaded {} policies", policies.size());
        return policies;
    }

    private List<RoleData> loadRoles() throws IOException {
        logger.info("Loading roles from baseline-security/roles.json");
        List<RoleData> roles = new ArrayList<>();

        try (InputStream is = new ClassPathResource("baseline-security/roles.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            for (JsonNode node : root) {
                RoleData role = new RoleData();
                role.setName(node.get("name").asText());
                role.setDescription(node.get("description").asText());

                List<String> policyNames = new ArrayList<>();
                JsonNode policiesNode = node.get("policies");
                if (policiesNode != null && policiesNode.isArray()) {
                    for (JsonNode policyNode : policiesNode) {
                        policyNames.add(policyNode.asText());
                    }
                }
                role.setPolicyNames(policyNames);

                roles.add(role);
            }
        }

        logger.info("Loaded {} roles", roles.size());
        return roles;
    }

    private List<PrincipalData> loadPrincipals() throws IOException {
        logger.info("Loading principals from baseline-security/principals.json");
        List<PrincipalData> principals = new ArrayList<>();

        try (InputStream is = new ClassPathResource("baseline-security/principals.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            for (JsonNode node : root) {
                PrincipalData principal = new PrincipalData();
                principal.setName(node.get("name").asText());
                principal.setPrincipalType(node.get("principalType").asText());
                principal.setDescription(node.get("description").asText());

                List<String> roleNames = new ArrayList<>();
                JsonNode rolesNode = node.get("roles");
                if (rolesNode != null && rolesNode.isArray()) {
                    for (JsonNode roleNode : rolesNode) {
                        roleNames.add(roleNode.asText());
                    }
                }
                principal.setRoleNames(roleNames);

                principals.add(principal);
            }
        }

        logger.info("Loaded {} principals", principals.size());
        return principals;
    }

    private List<UserData> loadUsers() throws IOException {
        logger.info("Loading users from baseline-security/users.json");
        List<UserData> users = new ArrayList<>();

        try (InputStream is = new ClassPathResource("baseline-security/users.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            for (JsonNode node : root) {
                UserData user = new UserData();
                user.setUsername(node.get("username").asText());
                user.setPassword(node.get("password").asText());

                JsonNode emailNode = node.get("email");
                if (emailNode != null && !emailNode.isNull()) {
                    user.setEmail(emailNode.asText());
                }

                user.setFirstName(node.get("firstName").asText());
                user.setSurname(node.get("surname").asText());

                JsonNode deptNode = node.get("department");
                if (deptNode != null && !deptNode.isNull()) {
                    user.setDepartment(deptNode.asText());
                }

                user.setEnabled(node.get("enabled").asBoolean());

                List<String> principalNames = new ArrayList<>();
                JsonNode principalsNode = node.get("principals");
                if (principalsNode != null && principalsNode.isArray()) {
                    for (JsonNode principalNode : principalsNode) {
                        principalNames.add(principalNode.asText());
                    }
                }
                user.setPrincipalNames(principalNames);

                users.add(user);
            }
        }

        logger.info("Loaded {} users", users.size());
        return users;
    }

    // Data classes
    public static class PolicyData {
        private String name;
        private String description;
        private List<String> roleNames;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getRoleNames() { return roleNames; }
        public void setRoleNames(List<String> roleNames) { this.roleNames = roleNames; }
    }

    public static class RoleData {
        private String name;
        private String description;
        private List<String> policyNames;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getPolicyNames() { return policyNames; }
        public void setPolicyNames(List<String> policyNames) { this.policyNames = policyNames; }
    }

    public static class PrincipalData {
        private String name;
        private String principalType;
        private String description;
        private List<String> roleNames;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPrincipalType() { return principalType; }
        public void setPrincipalType(String principalType) { this.principalType = principalType; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getRoleNames() { return roleNames; }
        public void setRoleNames(List<String> roleNames) { this.roleNames = roleNames; }
    }

    public static class UserData {
        private String username;
        private String password;
        private String email;
        private String firstName;
        private String surname;
        private String department;
        private boolean enabled;
        private List<String> principalNames;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getSurname() { return surname; }
        public void setSurname(String surname) { this.surname = surname; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public List<String> getPrincipalNames() { return principalNames; }
        public void setPrincipalNames(List<String> principalNames) { this.principalNames = principalNames; }
    }
}
