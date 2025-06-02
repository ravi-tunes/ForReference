package com.example.kerberos;

import javax.annotation.PreDestroy;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * A Spring‐managed service that:
 *  1) Performs a JAAS “kinit” by reading from login.conf under the alias “SolaceKrbAuth.”
 *  2) Returns the logged‐in Subject so that downstream libraries (e.g. GSSAPI‐based Solace JMS) can pick it up.
 *  3) Gracefully logs out at shutdown.
 *
 * We add @Retryable so that if the first login attempt fails with a transient LoginException
 * (e.g. KDC temporarily unreachable), it will retry up to 3 times with a 2s backoff.
 */
@Service
public class KerberosAuthService {
    private static final Logger logger = LoggerFactory.getLogger(KerberosAuthService.class);

    /**
     * Holds the JAAS Subject returned by login(). We keep it around so that we can logout() at @PreDestroy.
     */
    private Subject subject;

    /**
     * Called by your application to “kinit” from the keytab/principal defined in login.conf.
     *
     * The “SolaceKrbAuth” alias must match the section name in login.conf.
     * This method will retry up to 3 times if a LoginException is thrown.
     */
    @Retryable(
        value = { LoginException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000)
    )
    public Subject loginWithKeytab() throws LoginException {
        // The alias “SolaceKrbAuth” must exist in login.conf:
        // SolaceKrbAuth {
        //   com.sun.security.auth.module.Krb5LoginModule required
        //     useKeyTab=true
        //     keyTab="/opt/creds/systemaccount.keytab"
        //     principal="systemaccount@MYCOMPANY.COM"
        //     storeKey=true
        //     doNotPrompt=true
        //     isInitiator=true
        //     refreshKrb5Config=true;
        // };
        //
        // Because javax.security.auth.useSubjectCredsOnly=false, the GSSAPI layer will pick up the ticket
        // from Subject when connecting to Solace over SASL/GSSAPI.
        //
        // We construct the LoginContext with no explicit CallbackHandler (keytab + principal come from login.conf).
        LoginContext lc = new LoginContext("SolaceKrbAuth");
        lc.login();

        subject = lc.getSubject();
        logger.info("Kerberos login succeeded, Subject principal(s) = {}", subject.getPrincipals());
        return subject;
    }

    /**
     * Invoked when the Spring context is shutting down. We do a logout() here so the TGT is
     * destroyed from the Subject—clean shutdown of the Kerberos session.
     */
    @PreDestroy
    public void destroySubject() {
        if (subject != null) {
            try {
                // The second argument (Subject) is optional; if omitted, JAAS will use the Subject internally stored
                LoginContext lc = new LoginContext("SolaceKrbAuth", subject);
                lc.logout();
                logger.info("Kerberos logout completed cleanly.");
            } catch (LoginException e) {
                logger.error("Kerberos logout failed", e);
            }
        }
    }
}