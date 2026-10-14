package edu.hcmute.careersim.identity.access;

/** Trusted recipient resolver SPI; caller cannot select another enterprise. */
public interface SharingRecipientAccess {
    long requireEnterprise(long actor);
}
