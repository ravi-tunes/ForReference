@Configuration
@ConfigurationProperties(prefix = "kerberos")
public class KerberosConfig {
    private String principal;
    private String keytabLocation;
    private String krb5Conf;

    @PostConstruct
    public void init() {
        System.setProperty("java.security.krb5.conf", krb5Conf);
        System.setProperty("sun.security.krb5.debug", "true"); // Debug if needed
    }

    // Getters and setters
}