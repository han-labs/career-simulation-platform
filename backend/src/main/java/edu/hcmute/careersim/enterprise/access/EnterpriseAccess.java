package edu.hcmute.careersim.enterprise.access;

public interface EnterpriseAccess {
    Membership requireMember(long actor, long enterprise);

    Membership requirePublisher(long actor, long enterprise);

    Membership mine(long actor);

    record Membership(
            String enterpriseId,
            String name,
            String verificationStatus,
            String publishingStatus,
            String membershipStatus,
            boolean publishingPermission,
            long revision,
            long membershipRevision) {}
}
