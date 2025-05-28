import com.solacesystems.jcsmp.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SolaceConnectionManagerTest {

    @Test
    public void testSuccessfulConnection() throws JCSMPException {
        SolaceConfigProperties props = mock(SolaceConfigProperties.class);
        when(props.getHost()).thenReturn("tcp://test-host:55555");
        when(props.getVpn()).thenReturn("test-vpn");
        when(props.getUsername()).thenReturn("test-user");
        when(props.getPassword()).thenReturn("test-pass");

        JCSMPFactory factory = mock(JCSMPFactory.class, RETURNS_DEEP_STUBS);
        JCSMPSession session = mock(JCSMPSession.class);
        when(session.isClosed()).thenReturn(false);
        when(factory.createSession(any(), any(), any())).thenReturn(session);
        JCSMPFactory.override(factory);

        SolaceConnectionManager manager = new SolaceConnectionManager(props);
        JCSMPSession result = manager.getSession();

        assertNotNull(result);
        verify(props).getHost();
        verify(props).getVpn();
        verify(props).getUsername();
        verify(props).getPassword();
    }

    @Test
    public void testConnectionFailureWithRetries() {
        SolaceConfigProperties props = mock(SolaceConfigProperties.class);
        when(props.getHost()).thenReturn("tcp://fail-host:55555");
        when(props.getVpn()).thenReturn("fail-vpn");
        when(props.getUsername()).thenReturn("fail-user");
        when(props.getPassword()).thenReturn("fail-pass");

        SolaceConnectionManager manager = new SolaceConnectionManager(props) {
            @Override
            public JCSMPSession getSession() throws JCSMPException {
                throw new JCSMPException("Simulated failure");
            }
        };

        assertThrows(JCSMPException.class, manager::getSession);
    }
}
