/**
 * POJO representing a row from the SDS-FISS mapping table.
 */
public class SdsFissMapping {
    private final long sdsId;
    private final String fissId;

    public SdsFissMapping(long sdsId, String fissId) {
        this.sdsId = sdsId;
        this.fissId = fissId;
    }

    public long getSdsId() {
        return sdsId;
    }

    public String getFissId() {
        return fissId;
    }
}
