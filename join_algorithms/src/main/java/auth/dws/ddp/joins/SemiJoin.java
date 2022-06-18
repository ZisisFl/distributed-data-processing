package auth.dws.ddp.joins;

import auth.dws.ddp.db.RedisConnectionConfig;
import auth.dws.ddp.db.RedisHandler;

import java.util.List;

public class SemiJoin {
    public static void main(String[] args) {
        RedisConnectionConfig redisConnectionConfig = new RedisConnectionConfig();
        RedisHandler redis1 = new RedisHandler(redisConnectionConfig.redis1Host, redisConnectionConfig.redis1Port);
        RedisHandler redis2 = new RedisHandler(redisConnectionConfig.redis2Host, redisConnectionConfig.redis2Port);

        long startTime = System.currentTimeMillis();

        // get keys of the small relation
        List<String> keysOfSmallRelation = getKeysOfSmallRelation(redis1, redis2);

        semiJoin(keysOfSmallRelation, redis1, redis2);

        System.out.println("Execution time: " + (System.currentTimeMillis() - startTime) + " ms");

        redis1.close();
        redis2.close();
    }

    public static void semiJoin(List<String> keysOfSmallRelation, RedisHandler redis1, RedisHandler redis2) {
        // for each key in the small relation
        for (String key : keysOfSmallRelation) {
            // try to fetch value for each key
            String v1 = redis1.getData(key);
            String v2 = redis2.getData(key);

            // if key exists in both relations join them
            if (v1 != null && v2 != null) {

                System.out.printf("%s: (%s, %s)%n", key, v1, v2);
            }
        }
    }

    public static List<String> getKeysOfSmallRelation(RedisHandler redis1, RedisHandler redis2) {
        // get number of keys from both relations
        Long n_redis1_keys = redis1.getNumberOfKeys();
        Long n_redis2_keys = redis2.getNumberOfKeys();

        List<String> keysOfSmallRelation;

        // return keys of the smallest one
        if (n_redis1_keys > n_redis2_keys){
            keysOfSmallRelation = redis2.getKeys();
        }
        else if (n_redis1_keys < n_redis2_keys){
            keysOfSmallRelation = redis1.getKeys();
        }
        else {
            keysOfSmallRelation = redis1.getKeys();
        }

        return keysOfSmallRelation;
    }
}
