package auth.dws.ddp.db;

import redis.clients.jedis.resps.ScanResult;

import java.util.List;

public interface DBInterface {

    String getData(String key);
    List<String> getKeys();
    ScanResult<String> scan(String cursor);
    Long getNumberOfKeys();
    void putData(String key, String value);
    void delData(String key);
    void close();

}
