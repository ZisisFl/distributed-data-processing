package auth.dws.ddp.joins.utlis;

import java.util.Iterator;

public class Relation {
    private final String cursor;
    private final Iterator<String> keysIterator;
    private final Boolean fullyExplored;
    private final LatestKeyValuePair latestKeyValuePair;

    // we define Relation objects to represent the state of a relation while being scanned
    public Relation(String cursor, Iterator<String> keysIterator, LatestKeyValuePair latestKeyValuePair) {
        // latest cursor value
        this.cursor = cursor;
        // iterator of keys returned from scanning
        this.keysIterator = keysIterator;
        // latest key value pair fetched using keysIterator.next() method
        this.latestKeyValuePair = latestKeyValuePair;
        // check if relation is fully explored this value will be accessed to determine if we can keep
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
