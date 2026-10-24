package edu.hcmute.careersim.identity.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.hcmute.careersim.common.audit.AuditWriter;
import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.common.exception.ErrorCode;
import edu.hcmute.careersim.identity.access.*;
import edu.hcmute.careersim.identity.dao.IdentityDao;
import edu.hcmute.careersim.identity.dto.IdentityContracts.*;
import edu.hcmute.careersim.identity.service.impl.IdentityServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

class RegistrationServiceTest {
    IdentityDao dao = mock(IdentityDao.class);
    ActorAccess actors = mock(ActorAccess.class);
    ConsentService consents = mock(ConsentService.class);
    EnterpriseRegistration enterprises = mock(EnterpriseRegistration.class);
    PasswordEncoder passwords = mock(PasswordEncoder.class);
    AuditWriter audit = mock(AuditWriter.class);
    IdentityService service =
            new IdentityServiceImpl(dao, actors, consents, enterprises, passwords, audit);
    Registration request =
            new Registration(
                    "new@example.test", "synthetic-password-2026", "Student", "STUDENT", null);

    @Test
    void studentRegistrationCreatesProfileWithoutConsentOrEnterpriseGrant() {
        when(passwords.encode(request.password())).thenReturn("encoded-password");
        when(dao.register(request, "encoded-password")).thenReturn(7L);
        service.register(request);
        verify(dao).profile(7L);
        verify(dao).account(7L, List.of());
        verify(audit).record(eq(7L), eq("ACCOUNT_REGISTERED"), eq("app_users"), eq(7L), anyMap());
        verifyNoInteractions(consents, enterprises, actors);
    }

    @Test
    void duplicateEmailHasExplicitSignInGuidanceWithoutPartialFollowup() {
        when(passwords.encode(request.password())).thenReturn("encoded-password");
        when(dao.register(request, "encoded-password"))
                .thenThrow(new DuplicateKeyException("private SQL"));
        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("An account with this email already exists. Please sign in.")
                .satisfies(
                        e ->
                                assertThat(((ApiException) e).getErrorCode())
                                        .isEqualTo(ErrorCode.CONFLICT));
        verify(dao, never()).profile(anyLong());
        verifyNoInteractions(audit, consents, enterprises);
    }

    @Test
    void missingOrElevatedRoleCannotCreateAnAccount() {
        for (String role : new String[] {null, "ADMIN", "ADMINISTRATOR"}) {
            assertThatThrownBy(
                            () ->
                                    service.register(
                                            new Registration(
                                                    request.email(),
                                                    request.password(),
                                                    request.displayName(),
                                                    role,
                                                    null)))
                    .isInstanceOf(ApiException.class);
        }
        verifyNoInteractions(dao, passwords, enterprises, audit);
    }
}
