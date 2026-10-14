package edu.hcmute.careersim.common.storage;

/** The owning domain decides exact association/release access, never the file ID alone. */
public interface FileUseAuthorizer {
    boolean supports(String useType);

    void authorize(long actor, long asset, String useType, long useId);
}
