package auth.dws.ddp.joins;

import auth.dws.ddp.db.RedisConnectionConfig;
import auth.dws.ddp.db.RedisHandler;
import auth.dws.ddp.joins.utlis.BloomFilterConfig;

import java.nio.charset.StandardCharsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class IntersectionBloomFilterJoin {

    public static void main(String[] args) {
        RedisConnectionConfig redisConnectionConfig = new RedisConnectionConfig();
        RedisHandler redis1 = new RedisHandler(redisConnectionConfig.redis1Host, redisConnectionConfig.redis1Port);
        RedisHandler redis2 = new RedisHandler(redisConnectionConfig.redis2Host, redisConnectionConfig.redis2Port);

        long startTime = System.currentTimeMillis();

        // get keys of both relations
        List<String> keys1 = redis1.getKeys();
        List<String> keys2 = redis2.getKeys();

        // create union of keys from relations
        List<String> unionOfKeys = getUnionOfKeys(keys1, keys2);

        // configuration of bloom filters to be created
        // false positive prob of 3% will use 5 different Hash functions for the bloom filters
        BloomFilterConfig bf1Config = new BloomFilterConfig(keys1.size(), 0.03f);
        BloomFilterConfig bf2Config = new BloomFilterConfig(keys2.size(), 0.03f);
        BloomFilterConfig intersectionBFConfig = new BloomFilterConfig(keys1.size()+keys2.size(), 0.03f);

        BloomFilter<String> intersectionBF = generateIntersectionBF(keys1, keys2, bf1Config, bf2Config, intersectionBFConfig);

        intersectionBFJoin(intersectionBF, unionOfKeys, redis1, redis2);

        System.out.println("Execution time: " + (System.currentTimeMillis() - startTime) + " ms");

        redis1.close();
        redis2.close();
    }

    public static void intersectionBFJoin(BloomFilter<String> intersectionBF, List<String> unionOfKeys, RedisHandler redis1, RedisHandler redis2) {
        // for each key from the union of keys check if key exists in intersection bloom filter
        for (String key : unionOfKeys) {
            if (intersectionBF.mightContain(key)) {
                String v1 = redis1.getData(key);
                String v2 = redis2.getData(key);

                // in case the key actually exists in both relations make the join
                if (v1 != null && v2 != null) {
                    System.out.printf("%s: (%s, %s)%n", key, v1, v2);
                }
            }
        }
    }

    public static BloomFilter<String> createBloomFilter(List<String> keysList, BloomFilterConfig bfConfig) {
        // create bloom filter from relation's keys
        BloomFilter<String> bloomFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8),
                                                                                    bfConfig.getExpectedInsertions(),
                                                                                    bfConfig.getFalsePositiveProb());
        for (String key : keysList) {
            bloomFilter.put(key);
        }

        return bloomFilter;
    }

    public static BloomFilter<String> generateIntersectionBF(List<String> keys1, List<String> keys2,
                                                             BloomFilterConfig bf1Config,
                                                             BloomFilterConfig bf2Config,
                                                             BloomFilterConfig intersectionBFConfig) {
        // generate bloom filter for each relation's keys
        BloomFilter<String> bf1 = createBloomFilter(keys1, bf1Config);
        BloomFilter<String> bf2 = createBloomFilter(keys2, bf2Config);

        // init intersection bloom filter
        BloomFilter<String> intersectionBF = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8),
                                                                            intersectionBFConfig.getExpectedInsertions(),
                                                                            intersectionBFConfig.getFalsePositiveProb());

        // populate intersection bloom filter by finding common keys between each key set with the bloom filter of the other relation
        findPossibleCommonElements(keys1, bf2, intersectionBF);
        findPossibleCommonElements(keys2, bf1, intersectionBF);

        return intersectionBF;
    }

    public static void findPossibleCommonElements(List<String> keysList, BloomFilter<String> bloomFilter, BloomFilter<String> intersectionBF) {
        // find possible common elements between relation's keys and bloom filter of other relation
        // and insert them in intersection BF
        for (String key : keysList) {
            if (bloomFilter.mightContain(key)) {
                intersectionBF.put(key);
            }
        }
    }

    public static List<String> getUnionOfKeys(List<String> keys1, List<String> keys2) {
        // generate the union of all keys in both relations
        Set<String> unionOfKeys = new HashSet<>();

        unionOfKeys.addAll(keys1);
        unionOfKeys.addAll(keys2);

        return new ArrayList<>(unionOfKeys);
    }
}
