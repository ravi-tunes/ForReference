import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class SdsFissLookupServicePerformanceTest {

    private SdsFissLookupService service;

    @Setup(Level.Trial)
    public void setup() {
        SdsFissRepository repo = () -> {
            List<SdsFissMapping> list = new ArrayList<>(5_000_000);
            for (long i = 0; i < 4_000_000; i++) {
                list.add(new SdsFissMapping(i, null));
            }
            for (int i = 0; i < 1_000_000; i++) {
                list.add(new SdsFissMapping(10_000_000L + i, "FISS" + i));
            }
            return list;
        };

        service = new SdsFissLookupService(repo);
        service.initialize();
    }

    @Benchmark
    public Long testLookupBySdsId() {
        long id = ThreadLocalRandom.current().nextLong(0, 4_000_000);
        return service.resolveSdsId(Long.toString(id));
    }

    @Benchmark
    public Long testLookupByFissId() {
        int id = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return service.resolveSdsId("FISS" + id);
    }

    @Benchmark
    public Long testInvalidId() {
        return service.resolveSdsId("INVALID_ID");
    }
}
