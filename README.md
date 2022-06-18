# Join Algorithms
This project is about the implementation of three different join algorithms over key value pairs stored in Redis databases. The algorithms are implemented in Java.

## Pipelined Hash Join

## Semi Join


## Intersection Bloom Filter Join

# Usage
In order to start up the two Redis stores using docker compose execute the following command:
```sh
docker-compose up
```

By default a redis1 service will start at port `5555` and redis2 at port `6666`.

## Dependencies
This project makes use of some external libraries such as [jedis](https://github.com/redis/jedis) which is a java client for Redis and [guava](https://github.com/google/guava) library which provides an implementation of [BloomFilter](https://guava.dev/releases/20.0/api/docs/com/google/common/hash/BloomFilter.html). You ca find the specific dependencies in the pom.xml file of the maven project.