package auth.dws.ddp.joins;

import auth.dws.ddp.db.RedisHandler;
import redis.clients.jedis.resps.ScanResult;

import java.util.Hashtable;
import java.util.Iterator;


class Relation {
    private final String cursor;
    private final Iterator<String> keysIterator;
    private final Boolean fullyExplored;
    private final LatestKeyValuePair latestKeyValuePair;

    public Relation(String cursor, Iterator<String> keysIterator, LatestKeyValuePair latestKeyValuePair) {
        this.cursor = cursor;
        this.keysIterator = keysIterator;
        this.latestKeyValuePair = latestKeyValuePair;
        // check if relation is fully explored this value will be accesed to determine if we can keep
        // scanning this relation
        this.fullyExplored = cursor.equals("0") && !keysIterator.hasNext();
    }

    public String getCursor() {
        return cursor;
    }

    public Iterator<String> getKeysIterator() {
        return keysIterator;
    }

    public boolean getFullyExplored() {
        return fullyExplored;
    }

    public LatestKeyValuePair getLatestKeyValuePair() {
        return latestKeyValuePair;
    }
}

class LatestKeyValuePair {
    private final String key;
    private final String value;

    public LatestKeyValuePair(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}

public class PipelinedHashJoin {

    public static void main(String[] args) {
        RedisHandler redis1 = new RedisHandler("localhost", 5555);
        RedisHandler redis2 = new RedisHandler("localhost", 6666);

        long startTime = System.currentTimeMillis();

        pipelinedHashJoin(redis1, redis2, true);

        System.out.println("Execution time: " + (System.currentTimeMillis() - startTime) + " ms");

        redis1.close();
        redis2.close();
    }

    public static void pipelinedHashJoin(RedisHandler redis1, RedisHandler redis2, boolean startFromRelation1) {
        // init hash tables
        Hashtable<String, String> hash_table1 = new Hashtable<String, String>();
        Hashtable<String, String> hash_table2 = new Hashtable<String, String>();

        // init scanning of redis1
        ScanResult<String> res1 = redis1.scan("0");
        Relation relation1 = new Relation(res1.getCursor(), res1.getResult().iterator(), new LatestKeyValuePair(null, null));

        // init scanning of redis2
        ScanResult<String> res2 = redis2.scan("0");
        Relation relation2 = new Relation(res2.getCursor(), res2.getResult().iterator(), new LatestKeyValuePair(null, null));

        boolean readFromRelation1 = startFromRelation1;

        // iterate through redis relations, update hash tables and probe with key value pairs fetched
        while (!relation1.getFullyExplored() && !relation2.getFullyExplored()) {
            if (readFromRelation1 && !relation1.getFullyExplored()) {
                relation1 = updateHashTable(relation1.getCursor(), relation1.getKeysIterator(), redis1, hash_table1);
                probeAndJoin(relation1.getLatestKeyValuePair(), hash_table2);
            }
            else if (!readFromRelation1 && !relation2.getFullyExplored()) {
                relation2 = updateHashTable(relation2.getCursor(), relation2.getKeysIterator(), redis2, hash_table2);
                probeAndJoin(relation2.getLatestKeyValuePair(), hash_table1);
            }
            // change relation to read from
            readFromRelation1 = !readFromRelation1;
        }

        // if relation2 is fully explored continue with relation1
        while (!relation1.getFullyExplored()) {
            relation1 = updateHashTable(relation1.getCursor(), relation1.getKeysIterator(), redis1, hash_table1);
            probeAndJoin(relation1.getLatestKeyValuePair(), hash_table2);
        }

        // if relation1 is fully explored continue with relation2
        while (!relation2.getFullyExplored()) {
            relation2 = updateHashTable(relation2.getCursor(), relation2.getKeysIterator(), redis2, hash_table2);
            probeAndJoin(relation2.getLatestKeyValuePair(), hash_table1);
        }
    }

    public static Relation updateHashTable(String cursor, Iterator<String> keysIterator, RedisHandler redis, Hashtable<String, String> hashTable) {
        LatestKeyValuePair latestKeyValuePair;

        // check if there is a next item
        if (keysIterator.hasNext()) {
            // if item exists get key and query to fetch value and them in hash table
            latestKeyValuePair = fetchAndInsert(keysIterator, redis, hashTable);
        }
        else {
            // move cursor forward
            ScanResult<String> res = redis.scan(cursor);
            keysIterator = res.getResult().iterator();
            cursor = res.getCursor();

            // in case there are still next items
            if (keysIterator.hasNext()) {
                latestKeyValuePair = fetchAndInsert(keysIterator, redis, hashTable);
            }
            else {
                latestKeyValuePair = new LatestKeyValuePair(null, null);
            }
        }

        // return a Relation object to update relation state regarding cursor
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
