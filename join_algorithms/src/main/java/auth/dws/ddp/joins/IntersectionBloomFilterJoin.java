package auth.dws.ddp.joins;

import auth.dws.ddp.db.RedisHandler;

import java.nio.charset.StandardCharsets;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class IntersectionBloomFilterJoin {

    public static void main(String[] args) {
        RedisHandler redis1 = new RedisHandler("localhost", 5555);
        RedisHandler redis2 = new RedisHandler("localhost", 6666);

        long startTime = System.currentTimeMillis();

        List<String> keys1 = redis1.getKeys();
        List<String> keys2 = redis2.getKeys();
        List<String> unionOfKeys = getUnionOfKeys(keys1, keys2);

        BloomFilter<String> intersectionBF = generateIntersectionBF(keys1, keys2);

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

                if (v1 != null && v2 != null) {
                    System.out.printf("%s: (%s, %s)%n", key, v1, v2);
                }
            }
        }
    }

    public static BloomFilter<String> createBloomFilter(List<String> keysList) {
        // create bloom filter from relation's keys
        BloomFilter<String> bloomFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8),100, 0.03);
        for (String key : keysList) {
            bloomFilter.put(key);
        }

        return bloomFilter;
    }

    public static BloomFilter<String> generateIntersectionBF(List<String> keys1, List<String> keys2) {
        // generate bloom filter for each relation's keys and then their intersection
        BloomFilter<String> bf1 = createBloomFilter(keys1);
        BloomFilter<String> bf2 = createBloomFilter(keys2);

        BloomFilter<String> intersectionBF = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 100, 0.03);

        // populate intersection bloom filter by finding common keys between each key set and the bloom filter of the other relation
        findPossibleCommonElements(keys1, bf2, intersectionBF);
        findPossibleCommonElements(keys2, bf1, intersectionBF);

        return intersectionBF;
    }

    public static void findPossibleCommonElements(List<String> keysList, BloomFilter<String> bloomFilter, BloomFilter<String> intersectionBF) {
        // find possible common elements between relation's keys and bloom filter and insert them in intersection BF
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
