package auth.dws.ddp.db;

import java.util.List;

public interface DBInterface {

    String getData(String key);
    List<String> getKeys(String pattern);
    Long getNumberOfKeys();
    void putData(String key, String value);
    void delData(String key);
    void flushData();
    void close();

}
