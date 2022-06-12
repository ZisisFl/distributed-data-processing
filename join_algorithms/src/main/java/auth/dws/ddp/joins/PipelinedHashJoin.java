package auth.dws.ddp.joins;

import auth.dws.ddp.db.RedisHandler;
import redis.clients.jedis.resps.ScanResult;

import java.util.Hashtable;
import java.util.Iterator;


class Relation {
    private final String cursor;
    private final Iterator<String> keysIterator;
    private final Boolean fullyExplored;
    private final LatestTuple latestTuple;

    public Relation(String cursor, Iterator<String> keysIterator, LatestTuple latestTuple) {
        this.cursor = cursor;
        this.keysIterator = keysIterator;
        this.latestTuple = latestTuple;

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

    public LatestTuple getLatestTuple() {
        return latestTuple;
    }
}

class LatestTuple {
    private final String key;
    private final String value;

    public LatestTuple(String key, String value) {
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

        pipelinedHashJoin(redis1, redis2, true);
    }

    public static void pipelinedHashJoin(RedisHandler redis1, RedisHandler redis2, boolean startFromRelation1) {
        // init hash tables
        Hashtable<String, String> hash_table1 = new Hashtable<String, String>();
        Hashtable<String, String> hash_table2 = new Hashtable<String, String>();

        // init scanning of redis1
        ScanResult<String> res1 = redis1.scan("0");
        Relation relation1 = new Relation(res1.getCursor(), res1.getResult().iterator(), new LatestTuple(null, null));

        // init scanning of redis2
        ScanResult<String> res2 = redis2.scan("0");
        Relation relation2 = new Relation(res2.getCursor(), res2.getResult().iterator(), new LatestTuple(null, null));

        boolean readFromRelation1 = startFromRelation1;

        while (!relation1.getFullyExplored() && !relation2.getFullyExplored()) {
            if (readFromRelation1 && !relation1.getFullyExplored()) {
                relation1 = updateHashTable(relation1.getCursor(), relation1.getKeysIterator(), redis1, hash_table1);
                probeAndJoin(relation1.getLatestTuple(), hash_table2);
            }
            else if (!readFromRelation1 && !relation2.getFullyExplored()) {
                relation2 = updateHashTable(relation2.getCursor(), relation2.getKeysIterator(), redis2, hash_table2);
                probeAndJoin(relation2.getLatestTuple(), hash_table1);
            }
            readFromRelation1 = !readFromRelation1;
        }

        while (!relation1.getFullyExplored()) {
            relation1 = updateHashTable(relation1.getCursor(), relation1.getKeysIterator(), redis1, hash_table1);
            probeAndJoin(relation1.getLatestTuple(), hash_table2);
        }

        while (!relation2.getFullyExplored()) {
            relation2 = updateHashTable(relation2.getCursor(), relation2.getKeysIterator(), redis2, hash_table2);
            probeAndJoin(relation2.getLatestTuple(), hash_table1);
        }
    }

    public static Relation updateHashTable(String cursor, Iterator<String> keysIterator, RedisHandler redis, Hashtable<String, String> hashTable) {
        LatestTuple latestTuple;

        if (keysIterator.hasNext()) {
            latestTuple = fetchAndInsert(keysIterator, redis, hashTable);
        }
        else {
            ScanResult<String> res = redis.scan(cursor);
            keysIterator = res.getResult().iterator();
            cursor = res.getCursor();

            if (keysIterator.hasNext()) {
                latestTuple = fetchAndInsert(keysIterator, redis, hashTable);
            }
            else {
                latestTuple = new LatestTuple(null, null);
            }
        }

        return new Relation(cursor, keysIterator, latestTuple);
    }

    public static LatestTuple fetchAndInsert(Iterator<String> keysIterator, RedisHandler redis, Hashtable<String, String> hashTable) {
        String key = keysIterator.next();
        String value = redis.getData(key);

        hashTable.put(key, value);

        return new LatestTuple(key, value);
    }

    public static void probeAndJoin(LatestTuple latestTuple, Hashtable<String, String> hashTable) {
        String latestTupleKey = latestTuple.getKey();

        if (latestTupleKey != null) {
            String value = hashTable.get(latestTupleKey);

            if (value != null) {
                System.out.printf("%s: (%s, %s)%n", latestTupleKey, latestTuple.getValue(), value);
            }
        }
    }
}
