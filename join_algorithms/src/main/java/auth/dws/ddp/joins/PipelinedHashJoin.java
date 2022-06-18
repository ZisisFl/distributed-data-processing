package auth.dws.ddp.joins;

import auth.dws.ddp.db.RedisConnectionConfig;
import auth.dws.ddp.db.RedisHandler;
import auth.dws.ddp.joins.utlis.LatestKeyValuePair;
import auth.dws.ddp.joins.utlis.Relation;
import redis.clients.jedis.resps.ScanResult;

import java.util.Hashtable;
import java.util.Iterator;


public class PipelinedHashJoin {

    public static void main(String[] args) {
        RedisConnectionConfig redisConnectionConfig = new RedisConnectionConfig();
        RedisHandler redis1 = new RedisHandler(redisConnectionConfig.redis1Host, redisConnectionConfig.redis1Port);
        RedisHandler redis2 = new RedisHandler(redisConnectionConfig.redis2Host, redisConnectionConfig.redis2Port);

        long startTime = System.currentTimeMillis();

        pipelinedHashJoin(redis1, redis2, true);

        System.out.println("Execution time: " + (System.currentTimeMillis() - startTime) + " ms");

        redis1.close();
        redis2.close();
    }

    public static void pipelinedHashJoin(RedisHandler redis1, RedisHandler redis2, boolean startFromRelation1) {
        // init hash tables
        Hashtable<String, String> hashTable1 = new Hashtable<String, String>();
        Hashtable<String, String> hashTable2 = new Hashtable<String, String>();

        // init scanning of redis1
        ScanResult<String> res1 = redis1.scan("0");
        Relation relation1 = new Relation(res1.getCursor(), res1.getResult().iterator(), new LatestKeyValuePair(null, null));

        // init scanning of redis2
        ScanResult<String> res2 = redis2.scan("0");
        Relation relation2 = new Relation(res2.getCursor(), res2.getResult().iterator(), new LatestKeyValuePair(null, null));

        // set value to start reading from relation 1 or 2 first
        boolean readFromRelation1 = startFromRelation1;

        // iterate through redis relations, update hash tables and probe with keys fetched
        // while both of the relations are not fully explored (scan cursor back to 0)
        while (!relation1.getFullyExplored() && !relation2.getFullyExplored()) {
            if (readFromRelation1 && !relation1.getFullyExplored()) {
                // update hashTable1 with the latest key value pair fetched from relation1
                relation1 = updateHashTable(relation1.getCursor(), relation1.getKeysIterator(), redis1, hashTable1);
                // probe hashTable2 using the latest key fetched from relation1 and get possible join result
                probeAndJoin(relation1.getLatestKeyValuePair(), hashTable2);
            }
            else if (!readFromRelation1 && !relation2.getFullyExplored()) {
                relation2 = updateHashTable(relation2.getCursor(), relation2.getKeysIterator(), redis2, hashTable2);
                probeAndJoin(relation2.getLatestKeyValuePair(), hashTable1);
            }
            // change relation to read from at each iteration
            readFromRelation1 = !readFromRelation1;
        }

        // if relation2 is fully explored continue with relation1
        while (!relation1.getFullyExplored()) {
            relation1 = updateHashTable(relation1.getCursor(), relation1.getKeysIterator(), redis1, hashTable1);
            probeAndJoin(relation1.getLatestKeyValuePair(), hashTable2);
        }

        // if relation1 is fully explored continue with relation2
        while (!relation2.getFullyExplored()) {
            relation2 = updateHashTable(relation2.getCursor(), relation2.getKeysIterator(), redis2, hashTable2);
            probeAndJoin(relation2.getLatestKeyValuePair(), hashTable1);
        }
    }

    public static Relation updateHashTable(String cursor, Iterator<String> keysIterator, RedisHandler redis, Hashtable<String, String> hashTable) {
        LatestKeyValuePair latestKeyValuePair;

        // check if there is a next item
        if (keysIterator.hasNext()) {
            // if item exists get key and query to fetch value and put them in hash table
            latestKeyValuePair = fetchAndInsert(keysIterator, redis, hashTable);
        }
        else {
            // move cursor forward
            ScanResult<String> res = redis.scan(cursor);
            keysIterator = res.getResult().iterator();
            cursor = res.getCursor();

            // in case there are still next items get key and query to fetch value and put them in hash table
            if (keysIterator.hasNext()) {
                latestKeyValuePair = fetchAndInsert(keysIterator, redis, hashTable);
            }
            // else set key value record to null
            else {
                latestKeyValuePair = new LatestKeyValuePair(null, null);
            }
        }

        // return a Relation object to update relation state regarding cursor, iterator and latest key value pair
        return new Relation(cursor, keysIterator, latestKeyValuePair);
    }

    public static LatestKeyValuePair fetchAndInsert(Iterator<String> keysIterator, RedisHandler redis, Hashtable<String, String> hashTable) {
        // get key and value of latest item scanned
        String key = keysIterator.next();
        String value = redis.getData(key);

        hashTable.put(key, value);

        return new LatestKeyValuePair(key, value);
    }

    public static void probeAndJoin(LatestKeyValuePair latestKeyValuePair, Hashtable<String, String> hashTable) {
        // get key of the latest key value pair accessed
        String latestKey = latestKeyValuePair.getKey();

        // in case we fetched a key value pair in the latest call
        if (latestKey != null) {
            // probe using the key to the hast table of the other relation
            String value = hashTable.get(latestKey);

            // if key exists in hash table join values
            if (value != null) {
                System.out.printf("%s: (%s, %s)%n", latestKey, latestKeyValuePair.getValue(), value);
            }
        }
    }
}
