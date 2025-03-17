## Requirements
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
