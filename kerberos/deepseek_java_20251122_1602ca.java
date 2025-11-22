// ProgrammaticJaasConfig.java
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import java.util.HashMap;
import java.util.Map;

public class ProgrammaticJaasConfig extends Configuration {
    private final String username;

    public ProgrammaticJaasConfig(String username) {
        this.username = username;
    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        Map<String, String> options = new HashMap<>();
        options.put("principal", username);
        options.put("storeKey", "true");
        options.put("useTicketCache", "false");
        options.put("isInitiator", "true");
        options.put("refreshKrb5Config", "true");
        options.put("debug", "true"); // Enable for debugging, disable in production

        return new AppConfigurationEntry[]{
            new AppConfigurationEntry(
                "com.sun.security.auth.module.Krb5LoginModule",
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                options
            )
        };
    }
}