@Service
public class KerberosAuthService {
    private final String principal;
    private final String keytabLocation;

    public KerberosAuthService(
        @Value("${kerberos.principal}") String principal,
        @Value("${kerberos.keytab-location}") String keytabLocation
    ) {
        this.principal = principal;
        this.keytabLocation = keytabLocation;
    }

    public Subject loginWithKeytab() throws LoginException {
        LoginContext lc = new LoginContext("SolaceKrbAuth", new KrbCallbackHandler());
        lc.login();
        return lc.getSubject();
    }

    @Bean
    public Configuration jaasConfiguration() {
        return new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                Map<String, String> options = new HashMap<>();
                options.put("useKeyTab", "true");
                options.put("keyTab", keytabLocation);
                options.put("principal", principal);
                options.put("doNotPrompt", "true");
                options.put("storeKey", "true");
                options.put("isInitiator", "true");
                options.put("debug", "true");

                return new AppConfigurationEntry[]{
                    new AppConfigurationEntry(
                        "com.sun.security.auth.module.Krb5LoginModule",
                        LoginModuleControlFlag.REQUIRED,
                        options
                    )
                };
            }
        };
    }

    private static class KrbCallbackHandler implements CallbackHandler {
        @Override
        public void handle(Callback[] callbacks) {
            // No-op for keytab auth
        }
    }
}