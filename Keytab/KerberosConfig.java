package com.example.kerberos;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Reads the “kerberos.*” properties from application.yml (or application.properties)
 * and sets the corresponding system properties so that:
 *
 *  1) Java knows where to find krb5.conf
 *  2) JAAS knows where to find login.conf
 *
 * The keytabs and principal values themselves are not exposed here; they belong
 * in login.conf, which we point to via -Djava.security.auth.login.config.
 *
 * application.yml should contain:
 *
 *   kerberos:
 *     krb5-conf: /etc/krb5.conf
 *     login-config: /opt/app/login.conf
 *
 */
@Configuration
public class KerberosConfig {

    /**
     * Full path to your krb5.conf (realm/KDC definitions).
     */
    @Value("${kerberos.krb5-conf}")
    private String krb5Conf;

    /**
     * Full path to your JAAS login.conf (contains the “SolaceKrbAuth” section).
     */
    @Value("${kerberos.login-config}")
    private String loginConfig;

    @PostConstruct
    public void init() {
        // Point the JVM at krb5.conf so the KDC/realm lookup works
        System.setProperty("java.security.krb5.conf", krb5Conf);

        // Enable krb5 debugging if you need GSSAPI troubleshooting
        System.setProperty("sun.security.krb5.debug", "true");

        // Point JAAS at your on‐disk login.conf
        System.setProperty("java.security.auth.login.config", loginConfig);

        // Ensure that the Subject’s credentials are used by GSSAPI
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
    }
}