import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SolaceSender1PropertiesTest {

    @Test
    void testSettersAndGetters() {
        SolaceSender1Properties props = new SolaceSender1Properties();
        props.setHost("tcp://host1");
        props.setVpn("vpn1");
        props.setUsername("user1");
        props.setPassword("pass1");
        props.setDestinationType("topic");
        props.setDestinationName("test/topic1");

        assertEquals("tcp://host1", props.getHost());
        assertEquals("vpn1", props.getVpn());
        assertEquals("user1", props.getUsername());
        assertEquals("pass1", props.getPassword());
        assertEquals("topic", props.getDestinationType());
        assertEquals("test/topic1", props.getDestinationName());
    }
}
