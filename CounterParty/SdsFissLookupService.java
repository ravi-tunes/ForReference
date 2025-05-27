import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import java.sql.SQLException;
import java.util.List;

/**
 * Service for fast lookup of SDS IDs using either direct SDS ID or mapped FISS ID.
 */
public class SdsFissLookupService {

    private final SdsFissRepository repository;
    private final LongOpenHashSet sdsIdSet = new LongOpenHashSet();
    private final Object2LongOpenHashMap fissToSdsMap = new Object2LongOpenHashMap();
    private volatile boolean initialized = false;

    public SdsFissLookupService(SdsFissRepository repository) {
        this.repository = repository;
        this.fissToSdsMap.defaultReturnValue(-1L);
    }

    public synchronized void initialize() {
        if (initialized) return;

        try {
            List<SdsFissMapping> mappings = repository.fetchAllMappings();

            for (SdsFissMapping mapping : mappings) {
                sdsIdSet.add(mapping.getSdsId());
                if (mapping.getFissId() != null && !mapping.getFissId().isEmpty()) {
                    fissToSdsMap.put(mapping.getFissId(), mapping.getSdsId());
                }
            }

            initialized = true;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize SdsFissLookupService", e);
        }
    }

    public Long resolveSdsId(String incomingId) {
        if (!initialized || incomingId == null) return null;

        try {
            long id = Long.parseLong(incomingId);
            if (sdsIdSet.contains(id)) {
                return id;
            }
        } catch (NumberFormatException ignored) {}

        return fissToSdsMap.getLong(incomingId);
    }

    public boolean isInitialized() {
        return initialized;
    }
}
