# Join Algorithms
Join Algorithms is a simple Java maven project that implements three different join algorithms over key-value pairs stored in two different Redis databases. Other than the actual implementation of the algorithms in Java, the project provides the infrastructure and methods to provision it required to demonstrate the join operations. The deployment of the Redis stores can be achieved with the use of the provided docker-compose while there is also a Docker image to deploy join operations as Docker containers.

## Pipelined Hash Join
Pipelined Hash Join is a join algorithm that can work in streaming fashion. It constructs two hash tables one for each input. For each input tuple the hash table of the input is updated and the hash table of the other relation is probed in order to get a possible join pair. Compared to the simple Hash Join algorithm this one requires more memory as it needs to maintain two hash tables in memory instead of one.

## Semi Join
Semi Join algorithm is a special case of join where the keys of the smallest relation  (in terms of number of tuples) are retrieved and moved to the bigger relation in order to query it using the join keys to find the join pairs. As an operator Semi Join returns only the schema of the left relation for the join keys that it has in common with the right relation. In this implementation for the common keys we return the values of both key-value pairs as we do also in the other join algorithms. Semi join will save us a lot of computation time in cases where the small relation is relatively small or way smaller than the big one.

## Intersection Bloom Filter Join
Intersection Bloom Filter Join is a join variant that makes use of bloom filter probabilistic data structure to introduce intersection bloom filter which is the intersection result of two bloom filters each one populated with the join keys of each relation. This way we can probe the intersection bloom filter and quickly exclude keys from join candidates when they are not found in it. In case a key exists in the intersection bloom filter we are not 100% sure that it will produce a join pair, we need to further check if it exists in both relations. Bloom filters are also efficient in terms of memory usage as they only store bits.

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
If you need to change these ports, edit ports sections of both redis1 and redis2 services in the docker-compose file. Keep in mind though that if you want to run the code from your computer and not through a docker container these ports are defaults in the `auth.dws.dpp.db.RedisConnectionConfig` class which is used to establish the connection with Redis stores.

## Performing Join operations
You can use this project either by running the code through your IDE or by deploying docker containers of a custom image provided.

### Docker image
In order to build the docker image navigate to the `join_algorithms` directory of the project and use the following command:
```docker
docker build -t join-algos:0.1 .
```

The docker image contains the source code and the jar executebles created using maven assembly plugin as described in the pom.xml file. The jar files are configured to execute auth.dws.ddp.EntryPoint class which implements the logic required to deploy join algorithms as docker containers, performing all the environmental variable input handling and coresponding method executions.

### Docker containers
Other than the environmental variables that are required for data ingestion and join operations that will be discussed in the next sections, there are four variables that the user must provide each time that needs to deploy a docker container. Those variables are:
- REDIS1_HOST: Host of redis1 service, in this case that containers are meant to be deployed in the same bridge network it can be the name of the service 
- REDIS2_HOST: Host of redis2 service
- REDIS1_PORT: Port of redis1 service
- REDIS2_PORT: Port of redis2 service

If not set those variables default to localhost for hosts and ports 5555 and 6666 for redis1 and redis2 respectively. This is done in order to be easy to run and develop algorithms through IDE without the need of making any extra configurations.

In the next sections there will be example of docker container deployments.

### Ingesting key-value pairs to Redis
The first step required before performing the join operations between the two Redis stores is to populate them with actual key-value pairs. This can be done with many different ways. In this project there are two methods dedicated to this purpose. Both keys and values are expected to be of type string.

The class `auth/dws/ddp/db/DataIngestor.java` provides populateWithFixedKeys method which stores 
```json
["key_1", "key_2", "key_3", "key_4", "key_6"]
``` 
keys in redis1 store and 
```json
["key_1", "key_2", "key_3", "key_5", "key_6", "key_7", "key_8", "key_9", "key_10"]
```
 in redis2 store in both cases generating random values. This method was used primarly to develop the algorithms providing an easy way to check the join results. The other method provided is called populateWithRandomKeys which creates a pool of 50k keys and for each Redis store randomly picks 10k of them and ingests them with random values.

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
As long as the user has built the docker image he/she can use it to perform join operation. The container is flexible enough in terms of configuration to provide the ability to ingest data and perform a single join operation in a sinlge container deployment. This means that the user has to provide both a valid `JOIN_METHOD` and a valid `DATA_INGESTION_METHOD` through environmental variables when running the container.

#### Note1
Ingesting data to the Redis stores using the project provided methods will only upsert key-value pairs. The Redis stores **are not truncated** at each run of the algorithm.
In order flush data of redis1 service use the following command:
```docker
docker exec -it redis1 redis-cli FLUSHALL
```

Valid values of `JOIN_METHOD` are:
- semiJoin
- pipelinedHashJoin
- intersectionBloomFilterJoin

#### Semi Join
Example of ingesting data using populateWithRandomKeys methods and performing join with semiJoin method:
```docker
docker run --name join-algos --network=redis-network -e REDIS1_HOST=redis1 -e REDIS2_HOST=redis2 -e REDIS1_PORT=6379 -e REDIS2_PORT=6379 -e JOIN_METHOD=semiJoin -e DATA_INGESTION_METHOD=populateWithRandomKeys join-algos:0.1
```

#### Pipelined Hash Join  
In the case of Pipelined Hash Join a environmental variable can regulate from which relation should the algorithm start reading from. The variable is named `PHJStartFrom` and its valid values are `relation1` and `relation2` in order to start reading from the respective relation. If `PHJStartFrom` is not provided it defaults to relation1.

Example of performing pipelinedHashJoin without data ingestion and providing `PHJStartFrom` variable:
```docker
docker run --name join-algos --network=redis-network -e REDIS1_HOST=redis1 -e REDIS2_HOST=redis2 -e REDIS1_PORT=6379 -e REDIS2_PORT=6379 -e JOIN_METHOD=pipelinedHashJoin -e PHJStartFrom=relation2 join-algos:0.1
```

#### Intersection Bloom Filter Join
Example of performing intersectionBloomFilterJoin without data ingestion:
```docker
docker run --name join-algos --network=redis-network -e REDIS1_HOST=redis1 -e REDIS2_HOST=redis2 -e REDIS1_PORT=6379 -e REDIS2_PORT=6379 -e JOIN_METHOD=intersectionBloomFilterJoin join-algos:0.1
```
#### Note2
It is impossible to deploy a container with the same name multiple times for this reason you can change the container name each time your deploy and container by changing --name variable but this will use a lot of space in the disk as there will be multiple unused containers or you can delete the old container each time with the following command:
```docker
docker rm join-algos
```

#### Note3
In case of joining multiple key-values pairs, console will get full of messages (one message per joined key). You can pipe the output of the join operation in a text file like this.
```docker
docker run --name join-algos --network=redis-network -e REDIS1_HOST=redis1 -e REDIS2_HOST=redis2 -e REDIS1_PORT=6379 -e REDIS2_PORT=6379 -e JOIN_METHOD=semiJoin join-algos:0.1 |& tee phj.txt
``` 