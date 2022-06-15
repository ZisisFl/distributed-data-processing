package auth.dws.ddp.joins.utlis;

public class BloomFilterConfig {
    private final long expectedInsertions;
    private final float falsePositiveProb;

    public BloomFilterConfig(long expectedInsertions, float falsePositiveProb) {
        this.expectedInsertions = expectedInsertions;
        this.falsePositiveProb = falsePositiveProb;
    }

    public long getExpectedInsertions() {
        return expectedInsertions;
    }

    public float getFalsePositiveProb() {
        return falsePositiveProb;
    }
}
