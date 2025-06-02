@PreDestroy
public void destroySubject() {
    if (subject != null) {
        try {
            LoginContext lc = new LoginContext("SolaceKrbAuth", subject);
            lc.logout();
        } catch (LoginException e) {
            logger.error("Kerberos logout failed", e);
        }
    }
}