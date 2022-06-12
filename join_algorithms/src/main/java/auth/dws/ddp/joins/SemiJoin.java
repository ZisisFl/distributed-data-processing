package auth.dws.ddp.joins;

import auth.dws.ddp.db.RedisHandler;

import java.util.ArrayList;
import java.util.List;

public class SemiJoin {
    public static void main(String[] args) {
        RedisHandler redis1 = new RedisHandler("localhost", 5555);
        RedisHandler redis2 = new RedisHandler("localhost", 6666);

        List<String> keysOfSmallRelation = getKeysOfSmallRelation(redis1, redis2);

        semiJoin(keysOfSmallRelation, redis1, redis2);
    }

    public static void semiJoin(List<String> keysOfSmallRelation, RedisHandler redis1, RedisHandler redis2) {
        // for each key in the small relation
        for (String key : keysOfSmallRelation) {
            // try to fetch value for each key
            String v1 = redis1.getData(key);
            String v2 = redis2.getData(key);

            // if key exists in both relations join them
            if (v1 != null && v2 != null) {
                // create list of values
                List<String> values = new ArrayList<>();
                values.add(v1);
                values.add(v2);

                System.out.printf("%s: (%s, %s)%n", key, v1, v2);
            }
        }
    }

    public static List<String> getKeysOfSmallRelation(RedisHandler redis1, RedisHandler redis2) {
        Long n_redis1_keys = redis1.getNumberOfKeys();
        Long n_redis2_keys = redis2.getNumberOfKeys();

        List<String> keysOfSmallRelation;

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
