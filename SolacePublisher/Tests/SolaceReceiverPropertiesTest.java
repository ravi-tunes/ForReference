import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SolaceReceiverPropertiesTest {

    @Test
    void testSettersAndGetters() {
        SolaceReceiverProperties props = new SolaceReceiverProperties();
        props.setHost("tcp://host");
        props.setVpn("vpn");
        props.setUsername("user");
        props.setPassword("pass");
        props.setDestinationType("topic");
        props.setDestinationName("test/topic");

        assertEquals("tcp://host", props.getHost());
        assertEquals("vpn", props.getVpn());
        assertEquals("user", props.getUsername());
        assertEquals("pass", props.getPassword());
        assertEquals("topic", props.getDestinationType());
        assertEquals("test/topic", props.getDestinationName());
    }
}
