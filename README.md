## Requirements
* JDK 23.0.2 Corretto.
  If you use https://sdkman.io
  ```bash
  sdk install java 23.0.2-amzn
  ```
  [How to change JDK in IDE](https://www.jetbrains.com/help/idea/sdk.html#jdk)
* Docker

## How to run tests
```bash
./gradlew test
```

## How to run with docker-compose
```bash
docker-compose up
```

## Local run

### Launch db
```bash
docker run -d --name bookstore_local_db -e POSTGRES_USER=test -e POSTGRES_PASSWORD=test -e POSTGRES_DB=bookstore -p 5432:5432 postgres:17.4-alpine
```

### Launch app
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```
Navigate url: http://localhost:8080/swagger-ui/index.html

## About application
1. Implemented Reactive Spring Web to handle expected high load in production environments.
2. Utilized R2DBC driver for non-blocking database operations to improve scalability. 
3. Used BigDecimal for price and balance calculations to avoid floating-point precision issues.
4. Added automatic creation of 20 sample books in local environments for testing convenience.

### Future improvements
1. Configure CORS settings for production deployment.
2. Implement pagination for list operations to improve performance with large datasets.
3. Add security and authentication layers for protected endpoints.

