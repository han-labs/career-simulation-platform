package edu.hcmute.careersim.identity.access;

public interface ConsentAccess {
    boolean effective(long user, String purpose, String recipient);

    default void require(long user, String purpose, String recipient) {
        if (!effective(user, purpose, recipient))
            throw new edu.hcmute.careersim.common.exception.ApiException(
                    edu.hcmute.careersim.common.exception.ErrorCode.FORBIDDEN,
                    "An effective purpose-specific grant is required.");
    }
}
