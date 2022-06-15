package auth.dws.ddp.joins.utlis;

public class LatestKeyValuePair {
    private final String key;
    private final String value;

    // LatestKeyValuePair objects represent a key value pair fetched from a redis relation while scanning
    public LatestKeyValuePair(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
