package edu.hcmute.careersim.identity.access;

/** Registration transaction SPI, implemented by the organization-owning module. */
public interface EnterpriseRegistration {
    void createFor(long accountId, String enterpriseName);
}
