# Join Algorithms
This project is about the implementation of three different join algorithms over key-value pairs stored in two different Redis databases. The join algorithms are implemented in Java. The deployment of the Redis database can be achieved with the use of docker-compose provided while there is also a Docker image to deploy join operations os Docker containers.

## Pipelined Hash Join

## Semi Join

## Intersection Bloom Filter Join

# Dependencies
This project makes use of some external libraries such as [jedis](https://github.com/redis/jedis) which is a java client for Redis and [guava](https://github.com/google/guava) library which provides an implementation of [BloomFilter](https://guava.dev/releases/20.0/api/docs/com/google/common/hash/BloomFilter.html). You can find the specific dependencies in the pom.xml file of the maven project.

# Usage
## Starting Redis services
In order to start up the two Redis stores using docker compose execute the following command while being in root directory of the project:
```sh
docker-compose up
```

By default redis1 service will start at port `5555` and redis2 at port `6666`. 

#### Note
If you need to change these ports edit ports sections of both redis1 and redis2 services in the docker-compose file. Keep in mind though that if you want to run the code from your computer and not through a docker container these ports are defaults in the `auth.dws.dpp.db.RedisConnectionConfig` class which is used to establish the connection.

## Performing Join operations
You can use this project either by running the code through your IDE or by deploying docker containers of a custom image created.

### Docker image
In order to build the docker image use the following command:
```docker
docker build -t join-algos:0.1 .
```

### Ingesting key-value pairs to Redis
The first step required before performing the join operations between the two Redis stores is to populate them with actual key-value pairs. This can be done with many different ways. In this project there are two methods dedicated to this purpose.

The class `auth/dws/ddp/db/DataIngestor.java` provides populateWithFixedKeys method which stores ["key_1", "key_2", "key_3", "key_4", "key_6"] keys in redis1 store and ["key_1", "key_2", "key_3", "key_5", "key_6", "key_7", "key_8", "key_9", key_10"] in redis2 store. This method was used primarly to develop the algorithms providing an easy way to check the join results. The other method provided is called populateWithRandomKeys which creates a pool of 50k keys and for each Redis store randomly picks 10k of them and ingests them.

To perform data ingestion using the docker image created above the user can select the ingestion method by providing the appropriate value for the env variable `DATA_INGESTION_METHOD`.

Valid values of `DATA_INGESTION_METHOD` are:
- populateWithFixedKeys
- populateWithRandomKeys

Example for populateWithFixedKeys method:
```docker
docker run --name join-algos --network=redis-network -e REDIS1_HOST=redis1 -e REDIS2_HOST=redis2 -e REDIS1_PORT=6379 -e REDIS2_PORT=6379 -e DATA_INGESTION_METHOD=populateWithFixedKeys join-algos:0.1
```

Example for populateWithRandomKeys method:
```docker
docker run --name join-algos --network=redis-network -e REDIS1_HOST=redis1 -e REDIS2_HOST=redis2 -e REDIS1_PORT=6379 -e REDIS2_PORT=6379 -e DATA_INGESTION_METHOD=populateWithRandomKeys join-algos:0.1
```

### Picking a join algorithm
As long as the user has built the docker image he/she can use it to perform join operation. The container is flexibly enough in terms of configuration to provide the ability to ingest data and perform a single join operation in a sinlge container deployment. This means that the user has to provide both a valid `JOIN_METHOD` and a valid `DATA_INGESTION_METHOD` through environmental variables when running the container.

#### Note1
Ingesting data to the Redis stores using the project provided methods will only upsert key-value pairs. The Redis stores are not truncated at each run of the algorithm.

Valid values of `JOIN_METHOD` are:
- semiJoin
- pipelinedHashJoin
- intersectionBloomFilterJoin

Example of ingesting data using populateWithRandomKeys methods and performing join with semiJoin method:
```docker
docker run --name join-algos --network=redis-network -e REDIS1_HOST=redis1 -e REDIS2_HOST=redis2 -e REDIS1_PORT=6379 -e REDIS2_PORT=6379 -e JOIN_METHOD=semiJoin -e DATA_INGESTION_METHOD=populateWithRandomKeys join-algos:0.1
```

Pipelined Hash Join without data ingestion:
```docker
docker run --name join-algos --network=redis-network -e REDIS1_HOST=redis1 -e REDIS2_HOST=redis2 -e REDIS1_PORT=6379 -e REDIS2_PORT=6379 -e JOIN_METHOD=pipelinedHashJoin join-algos:0.1
```

Intersection BloomFilter Join without data ingestion:
```docker
docker run --name join-algos --network=redis-network -e REDIS1_HOST=redis1 -e REDIS2_HOST=redis2 -e REDIS1_PORT=6379 -e REDIS2_PORT=6379 -e JOIN_METHOD=intersectionBloomFilterJoin join-algos:0.1
```
#### Note2
It is impossible to deploy a container with the same name multiple times for this reason you can change the container name each time your deploy and container by changing --name variable but this will use a lot of space in the disk as there will be multiple unused containers or you can delete the old container each time with the following command:
```docker
docker rm join-algos
```

#### Note3
In case of joining multiple key-values pairs stdout will get full of messages (one message per joined key) you can pipe the output of the join operation in a text file like this.
```docker
docker run --name join-algos --network=redis-network -e REDIS1_HOST=redis1 -e REDIS2_HOST=redis2 -e REDIS1_PORT=6379 -e REDIS2_PORT=6379 -e JOIN_METHOD=semiJoin join-algos:0.1 |& tee phj.txt
``` 