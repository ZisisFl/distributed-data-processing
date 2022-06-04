package auth.dws.ddp.joins;

import auth.dws.ddp.db.RedisHandler;
import redis.clients.jedis.resps.ScanResult;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;


final class Relation {
    private final String cursor;
    private final Iterator<String> keysIterator;
    private final Boolean fullyExplored;

        public Relation(String cursor, Iterator<String> keysIterator) {
        this.cursor = cursor;
        this.keysIterator = keysIterator;

        if (cursor.equals("0") && !keysIterator.hasNext()) {
            this.fullyExplored = true;
        }
        else {
            this.fullyExplored = false;
        }
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
}

public class PipelinedHashJoin {

    public static Relation updateHashTable(String cursor, Iterator<String> keysIterator, RedisHandler redis, Hashtable<String, String> hashTable) {
        if (keysIterator.hasNext()) {
            fetchAndInsert(keysIterator, redis, hashTable);
        }
        else {
            ScanResult<String> res = redis.scan(cursor);
            keysIterator = res.getResult().iterator();
            cursor = res.getCursor();

            if (keysIterator.hasNext()) {
                fetchAndInsert(keysIterator, redis, hashTable);
            }
        }

        return new Relation(cursor, keysIterator);
    }

    public static void fetchAndInsert(Iterator<String> keysIterator, RedisHandler redis, Hashtable<String, String> hashTable) {
        String key = keysIterator.next();
        hashTable.put(key, redis.getData(key));
    }

    public static void main(String[] args) {
        RedisHandler redis1 = new RedisHandler("localhost", 5555);
        RedisHandler redis2 = new RedisHandler("localhost", 6666);

        Hashtable<String, String> hash_table1 = new Hashtable<String, String>();
        Hashtable<String, String> hash_table2 = new Hashtable<String, String>();

        boolean ended = false;

        // init HashMap to store results of joins
        HashMap<String, List<String>> joinResult = new HashMap<String, List<String>>();

        // start scanning of redis1
        ScanResult<String> res1 = redis1.scan("0");
        Relation relation1 = new Relation(res1.getCursor(), res1.getResult().iterator());

        // start scanning of redis2
        ScanResult<String> res2 = redis2.scan("0");
        Relation relation2 = new Relation(res2.getCursor(), res2.getResult().iterator());

        boolean readFromRelation1 = true;

        while (!ended) {
            if (readFromRelation1) {
                relation1 = updateHashTable(relation1.getCursor(), relation1.getKeysIterator(), redis1, hash_table1);
            }
            else {
                relation2 = updateHashTable(relation2.getCursor(), relation2.getKeysIterator(), redis2, hash_table2);
            }
            readFromRelation1 = !readFromRelation1;

            if (relation1.getFullyExplored() && relation2.getFullyExplored()){
                ended = true;
            }
        }

        System.out.println(hash_table1);
        System.out.println(hash_table2);
    }
}
