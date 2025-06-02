package com.example.kerberos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies that loginWithKeytab() constructs a LoginContext("SolaceKrbAuth") and calls login().
 * We use a static mock on LoginContext to intercept its constructor and confirm login() is invoked.
 */
@ExtendWith(MockitoExtension.class)
class KerberosAuthServiceTest {

    @InjectMocks
    private KerberosAuthService authService;

    @Test
    void loginSuccess() throws Exception {
        // Mock the constructor of LoginContext so it always returns our mock instance
        try (MockedStatic<LoginContext> lcStatic = mockStatic(LoginContext.class)) {
            LoginContext mockLc = mock(LoginContext.class);

            // When new LoginContext("SolaceKrbAuth") is invoked, return mockLc
            lcStatic
                .when(() -> new LoginContext(anyString()))
                .thenReturn(mockLc);

            // Call the method under test
            authService.loginWithKeytab();

            // Verify that we indeed invoked login() on mockLc
            verify(mockLc).login();
        }
    }
}