@Retryable(
    value = { LoginException.class },
    maxAttempts = 3,
    backoff = @Backoff(delay = 2000)
public Subject loginWithKeytab() throws LoginException {
    // login logic
}