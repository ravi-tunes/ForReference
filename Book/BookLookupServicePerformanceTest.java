import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class BookLookupServicePerformanceTest {

    private BookLookupService service;

    @Setup(Level.Trial)
    public void setUp() {
        BookRepository repo = () -> {
            Long2ObjectOpenHashMap<Book> map = new Long2ObjectOpenHashMap<>();
            for (long i = 0; i < 200_000; i++) {
                map.put(i, new Book(i, i + 1, 1, "CODE" + i, "ACC" + i));
            }
            return map.values().stream().toList();
        };
        service = new BookLookupService(repo);
        service.initialize();
    }

    @Benchmark
    public Book testGetById() {
        long id = ThreadLocalRandom.current().nextLong(0, 200_000);
        return service.getBookById(id);
    }

    @Benchmark
    public Book testGetByCode() {
        int id = ThreadLocalRandom.current().nextInt(0, 200_000);
        return service.getBookByCode("CODE" + id);
    }

    @Benchmark
    public Book testGetBySettlementAccount() {
        int id = ThreadLocalRandom.current().nextInt(0, 200_000);
        return service.getBookBySettlementAccount("ACC" + id);
    }

    @Benchmark
    public Book testGetByParentId() {
        long parentId = ThreadLocalRandom.current().nextLong(1, 200_001);
        return service.getBookByParentId(parentId);
    }
}
