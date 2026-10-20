package edu.hcmute.careersim.guidance.agent.service;

import static edu.hcmute.careersim.guidance.agent.dto.StudentAgentContracts.*;

import edu.hcmute.careersim.guidance.dto.PlanContracts.Plan;
import java.util.*;

public interface StudentAgentService {
    List<Session> sessions(
            long owner, UUID agent, java.time.Instant before, UUID beforeId, int limit);

    Session create(long owner, UUID agent, CreateSession request);

    Session session(long owner, UUID id);

    void delete(long owner, UUID id);

    List<Interaction> history(long owner, UUID session, long after, int limit);

    Run submit(long owner, UUID session, String task, Message request);

    Run read(long owner, UUID execution);

    Run cancel(long owner, UUID execution);

    List<Event> events(long owner, UUID execution, long after);

    List<Report> reports(long owner);

    Report report(long owner, UUID id);

    Plan confirm(long owner, ConfirmSuggestion request);
}
