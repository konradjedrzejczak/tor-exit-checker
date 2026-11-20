# Deployment to AWS

```
push to main ─► GitHub Actions: mvn verify ─► docker build ─► ECR (tag = git SHA)
                                   │                                  │
                                   └─► S3: test reports, build info   ▼
                              register task definition ─► ECS service (EC2 launch type)
                                                              │
                                              EC2 t3.micro + Elastic IP, port 80 ─► container :8080
```

| Service | What it is used for |
|---|---|
| EC2 | One `t3.micro` that runs the containers (ECS container instance) |
| ECS | Keeps the container running, replaces it on deploy, restarts it when the health check fails |
| ECR | Stores the Docker images (the last 5 are kept) |
| S3 | Archive of test reports and build info per commit (`builds/<sha>/`, removed after 30 days) |
| IAM (OIDC) | GitHub Actions gets short-lived credentials, no AWS keys are stored in GitHub |
| CloudWatch Logs | Container logs (`/ecs/tor-exit-checker`) |

Everything except the ECS service is described in `infrastructure.yml` (CloudFormation). The service is created by the
workflow on the first deployment. Estimated cost for a full month: about $15.

## One-time setup

1. CloudFormation → Create stack → upload `infrastructure.yml`. Parameter `GitHubRepo` = `owner/repo`.
   Set `CreateGitHubOidcProvider` to `false` only if the account already has a GitHub OIDC provider.
2. When the stack is ready, copy the output `GitHubActionsRoleArn`.
3. In GitHub → Settings → Secrets and variables → Actions → **Variables** add:
   `AWS_ROLE_ARN` (the output above) and `AWS_REGION` (the region of the stack).
4. Push to `main` (or run the workflow manually). The first deployment waits for the instance to join the cluster,
   so it can take a few minutes.

The workflow ends with a smoke test: `/actuator/health` must be `UP`, `192.0.2.1` must give `404`, `999.1.1.1` must give `400`.

## Removing everything

`AWS_REGION=eu-north-1 ./deploy/cleanup.sh`
