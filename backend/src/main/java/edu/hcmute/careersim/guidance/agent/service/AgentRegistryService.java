package edu.hcmute.careersim.guidance.agent.service;

import static edu.hcmute.careersim.guidance.agent.dto.AgentContracts.*;

import java.util.*;

public interface AgentRegistryService {
    List<Agent> list(long admin);

    Agent create(long admin, AgentInput input);

    Agent get(long admin, UUID id);

    Agent update(long admin, UUID id, long revision, AgentInput input);

    Agent archive(long admin, UUID id, long revision);

    List<Configuration> configurations(long admin, UUID agent);

    Configuration configuration(long admin, UUID agent, UUID config);

    Configuration save(long admin, UUID agent, long revision, ConfigurationInput input);

    Agent publish(long admin, UUID agent, UUID config, long revision, boolean confirmed);

    List<Assignment> assignments(long admin, UUID agent);

    EligibleStudents eligibleStudents(
            long admin, UUID agent, String query, long afterId, int limit);

    PublicationReadiness publicationReadiness(long admin, UUID agent, UUID config);

    Assignment assign(long admin, UUID agent, long student);

    Assignment revoke(long admin, UUID agent, UUID assignment);

    List<PublicAgent> assigned(long student);

    void requireAssignedConfiguration(long student, UUID agent, UUID config);
}
