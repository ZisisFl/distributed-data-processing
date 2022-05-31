package auth.dws.ddp.db;

import auth.dws.ddp.db.DBInterface;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.ArrayList;
import java.util.List;

public class RedisHandler implements DBInterface{
    private Jedis redis;

    public RedisHandler(String url, int port) {
        redis = new Jedis(url, port);
    }

    @Override
    public String getData(String key) {
        return redis.get(key);
    }

    @Override
    public List<String> getKeys(String pattern) {
        List<String> keys = new ArrayList<>();
        String cursor = "0";
        ScanParams sp = new ScanParams();
        //sp.match("*" + pattern + "*");
        //sp.count(1);
        //https://www.programcreek.com/java-api-examples/?class=redis.clients.jedis.Jedis&method=scan
        //https://redis.io/commands/scan/
        do {
            ScanResult<String> res = redis.scan(cursor, sp);
            List<String> result = res.getResult();
            if (result != null && result.size() > 0) {
                keys.addAll(result);
            }
            cursor = res.getCursor();
        } while (!cursor.equals("0"));
        return keys;
    }

    @Override
    public Long getNumberOfKeys() {
        return redis.dbSize();
    }

    @Override
    public void putData(String key, String value) {
        redis.set(key, value);
    }

    @Override
    public void delData(String key) {
        redis.del(key);
    }

    @Override
    public void flushData() {
        redis.flushDB();
    }

    @Override
    public void close() {
        redis.close();
        redis = null;
    }
}