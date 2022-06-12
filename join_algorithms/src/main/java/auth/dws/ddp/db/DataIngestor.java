package auth.dws.ddp.db;

import java.util.*;

public class DataIngestor {
    public static void main(String[] args) {
        RedisHandler redis1 = new RedisHandler("localhost", 5555);
        RedisHandler redis2 = new RedisHandler("localhost", 6666);

        // populate key stores with specific keys (and random values)
        populateWithFixedKeys(redis1, redis2);

        // populate each redis store with 1000 random key value pairs
        //populateWithRandomData(redis1, redis2);

        redis1.close();
        redis2.close();
    }

    public static void populateWithFixedKeys(RedisHandler redis1, RedisHandler redis2) {
        List<String> keys1 = Arrays.asList("key_1", "key_2", "key_3", "key_4", "key_6");

        generateKeyValuesPairFromList(redis1, keys1);

        List<String> keys2 = Arrays.asList("key_1", "key_2", "key_3", "key_5", "key_6",
                                            "key_7", "key_8", "key_9", "key_10");

        generateKeyValuesPairFromList(redis2, keys2);
    }

    public static void generateKeyValuesPairFromList(RedisHandler redis, List<String> keysList) {
        Random rand = new Random();

        for (String key : keysList) {
            redis.putData(key, String.valueOf(rand.nextInt(100000)));
        }
    }

    public static void populateWithRandomData(RedisHandler redis1, RedisHandler redis2) {
        List<String> keys = new ArrayList<>();

        // generate a list of 10k keys
        int numberOfRecords = 10000;
        for (int i=1;i<=numberOfRecords;i++) {
            keys.add("key_" + i);
        }

        // shuffle them and get put 1000 of them in redis
        Collections.shuffle(keys);
        generateKeyValuesPairFromList(redis1, keys.subList(0, 1000));

        // shuffle them again and get put 1000 of them in redis
        Collections.shuffle(keys);
        generateKeyValuesPairFromList(redis2, keys.subList(0, 1000));
    }
}
