import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SolaceIntegrationTest {

    @Autowired
    private SolaceConnectionManager receiverConnection;

    @Autowired
    private SolaceConnectionManager sender1Connection;

    @Autowired
    private SolaceConnectionManager sender2Connection;

    @Test
    public void testReceiverConnection() throws JCSMPException {
        JCSMPSession session = receiverConnection.getSession();
        assertNotNull(session);
        assertFalse(session.isClosed());
    }

    @Test
    public void testSender1Connection() throws JCSMPException {
        JCSMPSession session = sender1Connection.getSession();
        assertNotNull(session);
        assertFalse(session.isClosed());
    }

    @Test
    public void testSender2Connection() throws JCSMPException {
        JCSMPSession session = sender2Connection.getSession();
        assertNotNull(session);
        assertFalse(session.isClosed());
    }
}
