package auth.dws.ddp.joins;

import auth.dws.ddp.db.RedisHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SemiJoin {
    public static void main(String[] args) {
        RedisHandler redis1 = new RedisHandler("localhost", 5555);
        RedisHandler redis2 = new RedisHandler("localhost", 6666);

        List<String> keysOfSmallRelation = getKeysOfSmallRelation(redis1, redis2);
        System.out.println(keysOfSmallRelation);

        // init HashMap to store results of joins
        HashMap<String, List<String>> joinResult = new HashMap<String, List<String>>();

        // for each key in the small relation
        for (String key : keysOfSmallRelation) {
            // try to fetch value for each key
            String redis1_value = redis1.getData(key);
            String redis2_value = redis2.getData(key);

            // if key exists in both relations join them
            if (redis1_value != null && redis2_value != null) {
                // create list of values
                List<String> values = new ArrayList<>();
                values.add(redis1_value);
                values.add(redis2_value);

                // add key value result in Hashmap
                joinResult.put(key, values);
            }
        }
        System.out.println("Result of join:");
        System.out.println(joinResult);
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
