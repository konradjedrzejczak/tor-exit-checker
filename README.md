# Tor Exit Checker

A small HTTP service that tells whether an IPv4 address is a [Tor](https://www.torproject.org/) exit node,
based on the Tor Project's list: <https://check.torproject.org/exit-addresses>.

## API

| Request | Exit node | Not an exit node |
|---|---|---|
| `HEAD /A.B.C.D` | `200 OK`, empty body | `404 Not Found`, empty body |
| `GET /A.B.C.D` | `200 OK`, JSON body | `404 Not Found`, empty body |

Extra status codes: `400 Bad Request` if the path is not an IPv4 address, `503 Service Unavailable` if the exit list
has not been loaded yet.

Example `GET` response (`listUpdatedAt` says how fresh the data behind the answer is):

```json
{ "ip": "192.0.2.1", "listUpdatedAt": "2026-11-10T11:30:02Z" }
```

Health check: `GET /actuator/health` (`UP` once an exit list is loaded).

## Assumptions

- **Only IPv4.** Anything else (IPv6, hostnames, CIDR, `1.2.3`, `01.2.3.4`) gets `400`. The check is a strict regex;
  leading zeros are rejected because some systems read them as octal.
- **Exit node = the address appears in an `ExitAddress` line** of the Tor Project's list.
- **Caching:** the list is kept in memory and downloaded again after `cache-ttl` (default 30 minutes). A background
  job does this, so a request never waits for the Tor Project.
- **If a refresh fails**, the previous list keeps being used and the next try is one minute later. Other lines in the
  file and invalid addresses are ignored, and an empty download never replaces a good list.
- **`503`, not `404`, before the first successful download.** Answering "not an exit node" without any data would be
  a wrong answer for a security check.
- Authentication, TLS and rate limiting are out of scope (assumed to be handled in front of the service).

## Tech stack

- Java 21, Maven
- Spring Boot 3.5: Spring Web (REST), Actuator (health check), `@Scheduled` (background refresh)
- `RestClient` for the download
- JUnit 5, AssertJ, Mockito, MockMvc for tests
- Docker (multi-stage build)

## How to

Requirements: JDK 21 and Maven 3.9+ (or only Docker).

**Build**

```console
mvn package                          # builds target/tor-exit-checker.jar
docker build -t tor-exit-checker .   # builds the Docker image
```

**Run the automatic tests** (no internet access needed)

```console
mvn test
```

**Run locally**

```console
mvn spring-boot:run
# or
docker run --rm -p 8080:8080 tor-exit-checker

curl -I localhost:8080/192.0.2.1         # HEAD
curl -i localhost:8080/192.0.2.1         # GET
curl localhost:8080/actuator/health
```

Use a real address from <https://check.torproject.org/exit-addresses> to see a `200`. For a second or two after
start-up the service answers `503`, until the first download is done.

**Configuration** (`src/main/resources/application.yml`, can be overridden with environment variables, e.g.
`docker run -e TOR_EXIT_LIST_CACHE_TTL=PT15M ...`):

| Property | Default | Meaning |
|---|---|---|
| `tor.exit-list.url` | `https://check.torproject.org/exit-addresses` | where the list comes from |
| `tor.exit-list.cache-ttl` | `PT30M` | how long a downloaded list is used |
| `tor.exit-list.check-interval` | `PT1M` | how often the job checks whether a download is due |
| `tor.exit-list.connect-timeout` / `read-timeout` | `PT5S` / `PT20S` | HTTP timeouts |
