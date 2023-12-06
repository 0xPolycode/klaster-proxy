# Klaster Proxy

## Development setup

Minimum required Java version for running Gradle: 11

### Build

To build the application run `./gradlew build`

### Run

To run the application run `./gradlew bootRun`

## Tests

There are 3 tests sets:

- `test` for unit tests which do not start Spring Boot
- `integTest` for tests which run only partial Spring Boot
- `apiTest` for tests which run the entire Spring Boot application

To execute all tests run `./gradlew fullTest`
