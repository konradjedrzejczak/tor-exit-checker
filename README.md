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

- **Only IPv4.** Anything else (IPv6, hostnames, CIDR, `1.2.3`, `01.2.3.4`) gets `400`. Parsing is done by Guava
  (`InetAddresses`, which never does a DNS lookup) and an `Ipv4Address` type guarantees one canonical text form.
  Leading zeros are rejected because some systems read them as octal.
- **`HEAD` is answered by the `GET` handler;** Spring does that automatically and the web server leaves out the body.
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
- `RestClient` for the download, Guava for IP address parsing
- JUnit 5, AssertJ, Mockito, MockMvc for tests; the JDK's built-in `HttpServer` as a fake Tor Project
- Docker (multi-stage build)
- GitHub Actions (CI/CD), AWS: ECS on EC2, ECR, S3, CloudFormation, IAM with OIDC, CloudWatch Logs

### Structure

All classes are in one package, `com.example.torexit`:

| Class | Responsibility |
|---|---|
| `ExitNodeController` | `GET`/`HEAD /{ip}`: validates the address, asks the registry, picks the status code |
| `Ipv4Address` | a valid IPv4 address (created only through `parse`/`tryParse`) |
| `ExitNodeRegistry` | holds the current list in memory and answers lookups |
| `ExitListRefresher` | scheduled job; decides when the list must be downloaded again (TTL) and keeps the old list on failure |
| `ExitListLoader` | downloads (`ExitListClient`) and parses (`ExitListParser`) the list; fails if it contains no addresses |
| `TorExitProperties` | typed, validated settings (`tor.exit-list.*`) |
| `TorExitListHealthIndicator` | health check: `UP` once a list is loaded |

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

This runs unit tests, controller tests with MockMvc, and end-to-end tests that start the real application and
call it over HTTP, with a small local server in place of the Tor Project.

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

## Deployment (AWS)

Every push to `main` is tested, built into a Docker image and deployed to AWS by GitHub Actions
(`.github/workflows/ci-cd.yml`). Pull requests only run the tests.

```
push to main ─► GitHub Actions: mvn verify ─► docker build ─► ECR (image tag = git SHA)
                                   │                                  │
                                   └─► S3: test reports, build info   ▼
                              register task definition ─► ECS service (EC2 launch type)
                                                              │
                                              EC2 t3.micro + Elastic IP, port 80 ─► container :8080
```

| Service | What it is used for |
|---|---|
| EC2 | One `t3.micro` that runs the containers (an ECS container instance) |
| ECS | Keeps the container running, replaces it on every deploy, restarts it when the health check fails |
| ECR | Stores the Docker images (the last 5 are kept) |
| S3 | Archive of test reports and build info per commit (`builds/<git sha>/`, removed after 30 days) |
| IAM + OIDC | GitHub Actions gets short-lived credentials for one repository and one branch; no AWS keys are stored in GitHub |
| CloudWatch Logs | Container logs (`/ecs/tor-exit-checker`) |

Design choices:

- **ECS on EC2, not Fargate:** a similar price for one small container, and a fixed Elastic IP without a load balancer.
- **No load balancer, no NAT gateway:** one instance in a public subnet keeps the cost at roughly $15 a month.
  The price is a short pause during a deployment (the old container must stop before the new one can use port 80)
  and plain HTTP without a domain.
- **Infrastructure as code:** everything except the ECS service is described in `deploy/infrastructure.yml`
  (CloudFormation). The service is created by the workflow on the first deployment.
- **Health check:** ECS runs `GET /actuator/health` inside the container; the workflow ends with a smoke test of the
  public address (health is `UP`, `192.0.2.1` gives `404`, `999.1.1.1` gives `400`).

Setup and removal steps are in [deploy/README.md](deploy/README.md).
