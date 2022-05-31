package auth.dws.ddp.db;

import java.util.List;
import java.util.Random;

public class DataIngestor {
    public static void main(String[] args) {
        RedisHandler redis1 = new RedisHandler("localhost", 5555);
        RedisHandler redis2 = new RedisHandler("localhost", 6666);

        populateWithSampleData(redis1, redis2);
    }

    public static void populateWithSampleData(RedisHandler redis1, RedisHandler redis2) {
        // populate redis1
        generateData(redis1, 10);

        // populate redis2
        generateData(redis2, 6);
    }

    public static void generateData(RedisHandler redis, Integer numberOfRecords) {
        Random rand = new Random();

        for(int i=1;i<=numberOfRecords;i++){
            redis.putData("key_" + i, String.valueOf(rand.nextInt(1000)));
        }
    }

}
