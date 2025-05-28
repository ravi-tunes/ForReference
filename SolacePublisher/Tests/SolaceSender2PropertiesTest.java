import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SolaceSender2PropertiesTest {

    @Test
    void testSettersAndGetters() {
        SolaceSender2Properties props = new SolaceSender2Properties();
        props.setHost("tcp://host2");
        props.setVpn("vpn2");
        props.setUsername("user2");
        props.setPassword("pass2");
        props.setDestinationType("queue");
        props.setDestinationName("test/queue2");

        assertEquals("tcp://host2", props.getHost());
        assertEquals("vpn2", props.getVpn());
        assertEquals("user2", props.getUsername());
        assertEquals("pass2", props.getPassword());
        assertEquals("queue", props.getDestinationType());
        assertEquals("test/queue2", props.getDestinationName());
    }
}
