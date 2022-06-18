package auth.dws.ddp.db;

public class RedisConnectionConfig {
    final static String envRedis1Host = "REDIS1_HOST";
    final static String envRedis2Host = "REDIS2_HOST";
    final static String envRedis1Port = "REDIS1_PORT";
    final static String envRedis2Port = "REDIS2_PORT";

    public final String redis1Host;
    public final String redis2Host;
    public final int redis1Port;
    public final int redis2Port;

    public RedisConnectionConfig() {
        this.redis1Host = (System.getenv(envRedis1Host) != null) ? System.getenv(envRedis1Host) : "localhost";
        this.redis2Host = (System.getenv(envRedis2Host) != null) ? System.getenv(envRedis2Host) : "localhost";
        this.redis1Port = (System.getenv(envRedis1Port) != null && Integer.parseInt(System.getenv(envRedis1Port)) > 0)
                ? Integer.parseInt(System.getenv(envRedis1Port)) : 5555;
        this.redis2Port = (System.getenv(envRedis2Port) != null && Integer.parseInt(System.getenv(envRedis2Port)) > 0)
                ? Integer.parseInt(System.getenv(envRedis2Port)) : 6666;
    }
}
