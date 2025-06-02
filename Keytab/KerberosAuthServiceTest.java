@ExtendWith(MockitoExtension.class)
class KerberosAuthServiceTest {
    @Mock private Configuration jaasConfig;
    @InjectMocks private KerberosAuthService authService;

    @Test
    void loginSuccess() throws Exception {
        try (MockedStatic<LoginContext> lcMock = mockStatic(LoginContext.class)) {
            LoginContext mockLc = mock(LoginContext.class);
            lcMock.when(() -> new LoginContext(anyString(), any(Subject.class), any(CallbackHandler.class))
                .thenReturn(mockLc);
            
            authService.loginWithKeytab();
            verify(mockLc).login();
        }
    }
}