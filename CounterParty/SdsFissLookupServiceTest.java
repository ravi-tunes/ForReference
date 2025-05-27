import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SdsFissLookupServiceTest {

    private SdsFissRepository repository;
    private SdsFissLookupService service;

    @BeforeEach
    void setup() {
        repository = mock(SdsFissRepository.class);
        service = new SdsFissLookupService(repository);
    }

    @Test
    void testInitializationAndLookup() throws SQLException {
        List<SdsFissMapping> mappings = Arrays.asList(
                new SdsFissMapping(1001L, "FISS1001"),
                new SdsFissMapping(1002L, null),
                new SdsFissMapping(1003L, "FISS1003")
        );

        when(repository.fetchAllMappings()).thenReturn(mappings);
        service.initialize();

        assertTrue(service.isInitialized());
        assertEquals(1001L, service.resolveSdsId("1001"));
        assertEquals(1003L, service.resolveSdsId("FISS1003"));
        assertNull(service.resolveSdsId("9999"));
    }

    @Test
    void testNoMappings() throws SQLException {
        when(repository.fetchAllMappings()).thenReturn(Collections.emptyList());
        service.initialize();
        assertTrue(service.isInitialized());
        assertNull(service.resolveSdsId("1001"));
    }

    @Test
    void testInvalidIdFormat() throws SQLException {
        when(repository.fetchAllMappings()).thenReturn(Collections.singletonList(new SdsFissMapping(1234L, "FISS1234")));
        service.initialize();
        assertEquals(1234L, service.resolveSdsId("FISS1234"));
        assertNull(service.resolveSdsId("!!INVALID!!"));
    }

    @Test
    void testInitializationFailure() throws SQLException {
        when(repository.fetchAllMappings()).thenThrow(new SQLException("DB error"));
        assertThrows(RuntimeException.class, service::initialize);
    }
}
