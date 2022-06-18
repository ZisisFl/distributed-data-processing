package auth.dws.ddp;

import auth.dws.ddp.db.RedisConnectionConfig;
import auth.dws.ddp.db.RedisHandler;

import auth.dws.ddp.db.DataIngestor;
import auth.dws.ddp.joins.SemiJoin;
import auth.dws.ddp.joins.PipelinedHashJoin;
import auth.dws.ddp.joins.IntersectionBloomFilterJoin;

public class EntryPoint {
    final static String envDataIngestionMethod = "DATA_INGESTION_METHOD";
    final static String envJoinMethod = "JOIN_METHOD";
    final static String envPHJStartFrom = "PHJStartFrom";

    public static void main(String[] args) throws Exception {
        String joinMethod = System.getenv(envJoinMethod);
        String PHJStartFrom = (System.getenv(envPHJStartFrom) != null) ? System.getenv(envPHJStartFrom) : "relation1";

        RedisConnectionConfig redisConnectionConfig = new RedisConnectionConfig();
        RedisHandler redis1 = new RedisHandler(redisConnectionConfig.redis1Host, redisConnectionConfig.redis1Port);
        RedisHandler redis2 = new RedisHandler(redisConnectionConfig.redis2Host, redisConnectionConfig.redis2Port);

        if (System.getenv(envDataIngestionMethod) != null){
            DataIngestor.ingestData(redis1, redis2);
        }

        if (joinMethod != null) {
            long startTime = System.currentTimeMillis();

            switch (joinMethod) {
                case "semiJoin":
                    SemiJoin.semiJoin(redis1, redis2);
                    break;
                case "pipelinedHashJoin":
                    PipelinedHashJoin.pipelinedHashJoin(redis1, redis2, startFrom(PHJStartFrom));
                    break;
                case "intersectionBloomFilterJoin":
                    IntersectionBloomFilterJoin.intersectionBFJoin(redis1, redis2);
                    break;
            }

            System.out.println("Execution time: " + (System.currentTimeMillis() - startTime) + " ms");
        }
        else {
            System.out.println("You need to provide a join method through JOIN_METHOD env variable!");
        }
        redis1.close();
        redis2.close();
    }

    public static boolean startFrom(String PHJStartFrom) throws Exception {
        if (PHJStartFrom.equals("relation1")) {
            return true;
        }
        else if (PHJStartFrom.equals("relation2")) {
            return false;
        }
        else {
            throw new Exception("PHJStartFrom value can only be relation1 or relation2");
        }
    }
}
