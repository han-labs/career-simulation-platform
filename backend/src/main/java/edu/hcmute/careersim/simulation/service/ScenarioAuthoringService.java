package edu.hcmute.careersim.simulation.service;

import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import java.util.List;

public interface ScenarioAuthoringService {
    List<Version> versions(long actor);

    List<ParticipationSummary> participation(long actor, long enterprise);

    String slug(long actor, long version);

    Version create(long actor, Create request);

    Version cloneVersion(long actor, long scenario, Long source);

    Version get(long actor, long version);

    Version update(long actor, long version, long revision, Draft request);

    Version publish(long actor, long version, long revision);

    void availability(long actor, long scenario, long revision, String state);

    Classification classification(long actor, long scenario);

    Classification classify(long actor, long scenario, long revision, String industryCode);

    List<Skill> skills(long actor);

    String skill(long actor, SkillRequest request);
}
