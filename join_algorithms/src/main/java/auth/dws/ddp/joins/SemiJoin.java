package auth.dws.ddp.joins;

import auth.dws.ddp.db.RedisHandler;

import java.util.List;

public class SemiJoin {
    public static void main(String[] args) {
        RedisHandler redis1 = new RedisHandler("localhost", 5555);
        RedisHandler redis2 = new RedisHandler("localhost", 6666);

        Long redis1_keys = redis1.getNumberOfKeys();
        Long redis2_keys = redis2.getNumberOfKeys();

        List<String> keysOfSmallDB;

        if (redis1_keys > redis2_keys){
            keysOfSmallDB = redis2.getKeys("key_");
        }
        else if (redis1_keys < redis2_keys){
            keysOfSmallDB = redis1.getKeys("key_");
        }
        else {
            keysOfSmallDB = redis1.getKeys("key_");
        }
        System.out.println(keysOfSmallDB);
    }
}
