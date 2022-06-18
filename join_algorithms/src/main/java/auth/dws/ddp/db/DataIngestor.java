package auth.dws.ddp.db;

import java.util.*;

public class DataIngestor {
    final static String envDataIngestionMethod = "DATA_INGESTION_METHOD";
    public static void main(String[] args) throws Exception {
        RedisConnectionConfig redisConnectionConfig = new RedisConnectionConfig();
        RedisHandler redis1 = new RedisHandler(redisConnectionConfig.redis1Host, redisConnectionConfig.redis1Port);
        RedisHandler redis2 = new RedisHandler(redisConnectionConfig.redis2Host, redisConnectionConfig.redis2Port);

        String dataIngestionMethod = (System.getenv(envDataIngestionMethod) != null) ? System.getenv(envDataIngestionMethod) : "populateWithFixedKeys";

        if (Objects.equals(dataIngestionMethod, "populateWithFixedKeys")){
            // populate key stores with specific keys (and random values)
            System.out.println("Populating Redis stores with specific keys");
            populateWithFixedKeys(redis1, redis2);
        }
        else if (Objects.equals(dataIngestionMethod, "populateWithRandomKeys")) {
            // populate each redis store with 10000 random key-value pairs
            System.out.println("Populating each Redis store with 10000 key-value pairs");
            populateWithRandomKeys(redis1, redis2);
        }
        else {
            throw new Exception("Unknown data ingestion method it can be either populateWithFixedKeys or populateWithRandomKeys");
        }

        System.out.println("Data ingested successfully");

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

    public static void populateWithRandomKeys(RedisHandler redis1, RedisHandler redis2) {
        List<String> keys = new ArrayList<>();

        // generate a list of 50k keys
        int numberOfRecords = 50000;
        for (int i=1;i<=numberOfRecords;i++) {
            keys.add("key_" + i);
        }

        // shuffle them and put 10000 of them in redis
        Collections.shuffle(keys);
        generateKeyValuesPairFromList(redis1, keys.subList(0, 10000));

        // shuffle them again and put 10000 of them in the other redis store
        Collections.shuffle(keys);
        generateKeyValuesPairFromList(redis2, keys.subList(0, 10000));
    }
}
