package edu.hcmute.careersim.enterprise.access;

/** Organization display identity only; conveys no membership or publishing authority. */
public interface EnterpriseDirectoryAccess {
    String name(long enterprise);
}
