# kitu infrastructure

AWS CDK (TypeScript) app that provisions everything kielitutkintorekisteri needs.
The code under `lib/` defines the stacks; `bin/infra.ts` wires them into stages.

When you change anything in this directory, keep this file accurate — see CLAUDE.md.

## AWS account layout

| Stage  | Account        | Region(s)                 | Purpose                                                                |
| ------ | -------------- | ------------------------- | ---------------------------------------------------------------------- |
| `Util` | `961341524988` | `eu-west-1`               | Shared ECR repo + GitHub Actions OIDC trust + Util-level Slack alarms  |
| `Dev`  | `682033502734` | `eu-west-1` + `us-east-1` | Untuva/dev environment                                                 |
| `Test` | `961341546901` | `eu-west-1` + `us-east-1` | QA environment                                                         |
| `Prod` | `515966535475` | `eu-west-1` + `us-east-1` | Production. Reader replica, deletion protection, performance insights. |

The `us-east-1` footprint per env is small: Route53 health checks (must be global) and
a slim `AlarmsStack` that fronts them. Everything else is in `eu-west-1`.

## Project layout

```
infra/
├── bin/infra.ts                     # CDK app: instantiates Util + Dev/Test/Prod stages
└── lib/
    ├── accounts.ts                  # Account IDs, CIDR blocks, Slack channels, KOSKI accounts
    ├── env.ts                       # getEnv(): fetch required env var or throw
    ├── utility-stage.ts             # Util stage (composes the stacks below)
    ├── container-repository-stack.ts  # ECR repo `kitu` + cross-account pull grants
    ├── github-actions-stack.ts      # OIDC provider + per-env deploy role
    ├── environment-stage.ts         # Composes the stacks below per env
    ├── network-stack.ts             # VPC + subnets
    ├── connections-stack.ts         # Security groups (two-phase: createRules() at end)
    ├── db-stack.ts                  # Aurora Postgres ServerlessV2
    ├── service-stack.ts             # ECS Fargate service + ALB + OTel sidecar
    ├── dns-stack.ts                 # Looks up pre-existing Route53 zone
    ├── route53-health-checks-stack.ts  # us-east-1 health check + alarm
    ├── alarms-stack.ts              # SNS topics + Chatbot Slack + AIOps investigation group
    ├── log-groups-stack.ts          # Log groups + LogErrors/LogWarnings filters + alarms
    ├── backups-stack.ts             # AWS Backup vault for the Aurora cluster
    ├── koski-audit-logs-integration-stack.ts  # Subscription filter + Lambda → KOSKI SQS
    └── koski-audit-logs-integration/  # Lambda source for the above
```

## Util stage (`bin/infra.ts`, `lib/utility-stage.ts`)

Created once, in the shared util account. Three things live here:

- **`Util/ImageBuilds`** — ECR repo `kitu` with `IMMUTABLE` tags and scan-on-push.
  The repo grants pull to each env account principal and pull+push to the GitHub
  Actions deployment role. CI builds an image, pushes it here, then env deploys
  resolve the image from ECR by `TAG` env var (`bin/infra.ts:23-26`).
- **`Util/Alarms`** — its own `AlarmsStack` for image-scan findings + Util-level
  Slack notifications.
- **`Util/GithubActions`** — the OIDC provider and the deployment role assumed by
  the workflow `Opetushallitus/kielitutkintorekisteri`.

## Environment stages (`lib/environment-stage.ts`)

Each of `Dev`, `Test`, `Prod` instantiates the same set of stacks in roughly this
order (because of dependencies):

1. **`GithubActions`** — per-env CDK deploy role + CloudWatch log-tail permission.
   Trusted via OIDC.
2. **`AlarmsUsEast1`** (region: `us-east-1`) and **`Alarms`** (env region) — see
   the Monitoring section below. The us-east-1 stack is created first so the
   primary stack can subscribe its SNS topics to the single shared Slack
   `SlackChannelConfiguration` (Chatbot allows only one per Slack channel per
   account).
3. **`Dns`** — `HostedZone.fromLookup(...)` against a **pre-existing** Route53
   zone. The zone is created manually to avoid CDK ever destroying it.
4. **`LogGroups`** — `KituService` and `KituServiceAudit` log groups, the
   `KituServiceDataProtectionAudit` group (with a Finnish-SSN data-protection
   policy applied to the service log group), `LogErrors`/`LogWarnings` metric
   filters and alarms, and CloudWatch Transaction Search wiring for X-Ray spans.
5. **`Network`** — VPC with the per-env CIDR (Dev `10.15.0.0/18`, Test
   `10.15.64.0/18`, Prod `10.15.128.0/18`) and 2 AZs in dev / 3 AZs in test+prod.
6. **`Connections`** — three empty SGs (`serviceSG`, `loadBalancerSG`,
   `databaseSG`). Rules are added in `createRules()` at the very end of the
   stage, after the DB is created and `databaseSG` is populated. This deferred
   wiring avoids a circular dependency between stacks.
7. **`Database`** — Aurora Postgres ServerlessV2. Always one writer; prod
   additionally gets a reader replica with auto-scaling, deletion protection,
   and Performance Insights. The cluster is in private subnets and only the
   Service stack's task security group is granted ingress on `5432`. Direct
   developer access from a laptop is intentionally not wired today; if you
   need it, add a bastion or `aws ecs run-task`-based tunnel as a separate
   stack.
8. **`Service`** — the application Fargate service, see below.
9. **`Route53HealthChecks`** (region: `us-east-1`) — HTTPS check against the
   public domain; alarm wired to `usEastAlarmsStack.investigationAction`.
10. **`Backups`** — AWS Backup plan that protects the Aurora cluster; backup-job
    state changes go to the alarm SNS topic.
11. **`KoskiAuditLogsIntegration`** — a CloudWatch Logs subscription filter on
    the audit log group → Lambda → SQS in the KOSKI account. The Lambda assumes
    a role (`kitu-sqs-sender`) that the KOSKI account creates with a matching
    trust policy.

## Service stack details (`lib/service-stack.ts`)

- ALB with HTTP→HTTPS redirect; certificate from ACM with DNS validation against
  the looked-up zone.
- Task definition: 2048 CPU, 4096 MiB memory; deployment circuit breaker with
  rollback enabled.
- Container image: resolved by `TAG` env var; image lives in the Util account
  ECR repo and is pulled across accounts via the resource policy granted in
  `container-repository-stack.ts`.
- Secrets pulled at task start from Secrets Manager: `kielitesti-token`,
  `palvelukayttaja-password`, `yki-api-password`, `yki-api-user`,
  `palvelukayttaja-oauth-password`. **These must exist in the env account
  before deploy** — CDK doesn't create them. The DB password is sourced
  from the auto-generated cluster secret.
- OTel sidecar: collector config stored as an SSM parameter
  (`/kitu/otel-config-<stack-id>`), loaded into the sidecar via
  `AOT_CONFIG_CONTENT`. Tail sampling: 0.1% for the health check route,
  always-sample for everything else. Traces export to X-Ray; metrics export
  to CloudWatch via EMF.
- Health check: 2 successful pings × 10s = 20s to healthy. Deployment drain
  timeout 5s.
- Alarms attached: 5xx count ≥ 4 over the period; CPU > 50%; memory > 50%.
  All three trigger SNS + investigation.
- An S3 bucket for `tehtavapankki` is created and the task role gets
  read+write access to it (write for the Koealusta-import scheduled task,
  read for the virkailija-facing listing page and signed download URLs).

## Monitoring, alarms, investigations, Slack

How CloudWatch alarms, Amazon Q investigations, log-based metrics, and the Slack
feed fit together.

### End-to-end flow

```
log line → metric filter → CloudWatch metric → CloudWatch alarm
                                                  │
                                                  ├─► SNS topic ─► Chatbot ─► Slack
                                                  │
                                                  └─► AIOps investigation group
                                                                ├─► SNS topic ─► Chatbot ─► Slack
                                                                └─► Q-curated message ─► Chatbot ─► Slack
```

When an alarm transitions to `ALARM`:

1. CloudWatch publishes the alarm payload to the SNS alarm topic. AWS Chatbot is
   subscribed to that topic and forwards a templated message to Slack.
2. The same alarm has the AIOps investigation group ARN as a second action.
   CloudWatch calls `aiops:CreateInvestigation` on the group (allowed by the
   group's resource policy that grants
   `aiops.alarms.cloudwatch.amazonaws.com`).
3. Q investigates in the background. While doing so, it pushes events both to
   the investigation group's SNS topic and directly to the configured chat
   channels (`chatConfigurationArns`). The latter is what produces the
   Q-curated summary in Slack.

### Stack layout

Per env (`Dev` / `Test` / `Prod`):

- **`<Env>/AlarmsUsEast1`** — SNS topics + investigation group in `us-east-1`.
  Used exclusively by Route53 health checks. It does _not_ own a Chatbot Slack
  channel; instead its SNS topics are subscribed to the primary stack's Slack
  channel via `additionalAlarmTopics` / `additionalInfoTopics`. (Chatbot only
  allows one `SlackChannelConfiguration` per channel per account, so the
  primary stack owns it and us-east-1 piggybacks.) Its investigation group is
  _not_ wired to a Slack chat configuration — Route53 investigations only emit
  SNS notifications. If more alarms are ever added in `us-east-1`, this needs
  to be revisited.
- **`<Env>/Alarms`** — Primary alarm infrastructure in the env's region. Owns
  the Chatbot `SlackChannelConfiguration`, the Investigation group with both
  SNS and Slack notification channels, and exposes
  `alarmSnsTopic` / `infoSnsTopic` / `investigationAction` for other stacks to
  attach to.
- **`<Env>/LogGroups`** — Service log groups + metric filters that turn
  structured log lines into CloudWatch metrics, and the `LogErrors` /
  `LogWarnings` alarms wired to those topics + investigation action.

### Slack channels

Per `lib/accounts.ts`:

| Env  | Alarms channel                           | Info channel             |
| ---- | ---------------------------------------- | ------------------------ |
| dev  | `kielitutkintorekisteri-alerts-dev-test` | —                        |
| test | `kielitutkintorekisteri-alerts-dev-test` | —                        |
| prod | `kielitutkintorekisteri-alerts`          | `kielitutkintorekisteri` |

Workspace `T02C6SZL7KP` for all envs.

### IAM identities involved

Three roles, easy to confuse:

1. **Investigation group role** (`InvestigationGroupRole` in
   `alarms-stack.ts`). Assumed by `aiops.amazonaws.com`. Has
   `AIOpsAssistantPolicy` (managed). Used by Q itself to read service
   telemetry while running an investigation.
2. **Chatbot Slack channel role** (`SlackBot/ConfigurationRole`, auto-created
   by the CDK construct). Assumed by `chatbot.amazonaws.com`. We attach
   `AIOpsAssistantPolicy` to it explicitly — without that, Q can't call
   `aiops:GetInvestigation*` to fetch the data needed to render the curated
   Slack message, and the channel stays silent even though
   `chatConfigurationArns` is wired correctly.
3. **Investigation group resource policy** (`investigationGroupPolicy` on the
   `CfnInvestigationGroup`). Grants `aiops.alarms.cloudwatch.amazonaws.com`
   the right to call `CreateInvestigation` / `CreateInvestigationEvent` on
   this group, scoped to alarms in the same account/region. Without this,
   alarms silently fail to start investigations.

### Log-based metric filters (`lib/log-groups-stack.ts`)

The application logs JSON in ECS format
(`logging.structured.format.console=ecs`, level field is `log.level`).

- **`LogErrors`** — counts entries where `$.success == false` _or_
  `$.log.level == "ERROR"`. Threshold: **1**, evaluation period 1. Triggers
  SNS alarm + investigation. Earlier versions also matched on the existence
  of `$.stack_trace` / `$.error.type`; that produced false positives because
  Spring Boot includes those fields in any WARN log that carries a
  `Throwable` (SpringDoc and OTel emit such warnings during startup).
- **`LogWarnings`** — counts entries where `$.log.level == "WARN"`.
  Threshold: **5**.

Add new metric/alarm pairs through the same
`addMetricFilter(...).metric(...).createAlarm(...)` chain and remember to
attach `SnsAction(alarmsSnsTopic)` and `investigationAction` so they reach
Slack.

### Verifying live state

```bash
# Investigation groups (note: aws cli ≥ 2.34 required for `aws aiops`)
aws aiops list-investigation-groups --region eu-west-1 --profile <env>
aws aiops get-investigation-group --identifier <name> --region eu-west-1 --profile <env> \
  --query 'chatbotNotificationChannel'

# Chatbot Slack channel role + attached policies
aws chatbot describe-slack-channel-configurations --region us-east-1 --profile <env> \
  --query 'SlackChannelConfigurations[?ConfigurationName==`<channel-name>`].IamRoleArn'
aws iam list-attached-role-policies --role-name <role-from-above> --profile <env>
```

## KOSKI audit logs integration

The service's audit log group has a CloudWatch Logs subscription filter
(`anyTerm("operation")`) that fires a Lambda. The Lambda parses the log event,
assumes a role in the KOSKI account
(`arn:aws:iam::<koski-account>:role/kitu-sqs-sender`), and pushes to the SQS
queue `oma-opintopolku-loki-audit-queue` in the KOSKI account. KOSKI manages
both the role's trust policy (allowing the kitu Lambda role to assume it) and
the queue itself. Per env:

| Env  | KOSKI account  | KOSKI region |
| ---- | -------------- | ------------ |
| dev  | `500150530292` | `eu-west-1`  |
| test | `692437769085` | `eu-west-1`  |
| prod | `508832528142` | `eu-west-1`  |

## Deploying

CI (`/.github/workflows/build.yml`) auto-deploys main: builds the image, pushes
it to Util ECR, then runs `cdk deploy` against Dev → Test → Prod with the new
`TAG`.

For local diffs/deploys, the `infra/package.json` scripts cover dev:

```bash
npm run diff/dev
npm run deploy/dev
```

For other envs, invoke cdk directly with `TAG` set. Narrow with `--exclusively`
when you only want to touch a few stacks:

```bash
TAG=$(git rev-parse main) npx cdk diff   --exclusively 'Prod/Alarms' 'Prod/AlarmsUsEast1' 'Prod/LogGroups'
TAG=$(git rev-parse main) npx cdk deploy --exclusively 'Prod/Alarms' 'Prod/AlarmsUsEast1' 'Prod/LogGroups'
```

CDK bootstrap (`cdk-hnb659fds-*` roles) must already exist in each account; the
GitHub Actions deploy role assumes those roles to publish artifacts.

## Manual prerequisites that don't live in CDK

Listed here because deploys silently or loudly fail when these don't exist:

- **Route53 hosted zone** per env (`kios.<domain>`). Looked up by name in
  `dns-stack.ts`; not created by CDK to prevent accidental destruction.
- **Secrets Manager secrets** per env account, by exact name:
  `kielitesti-token`, `palvelukayttaja-password`, `yki-api-password`,
  `yki-api-user`, `palvelukayttaja-oauth-password`,
  `oppijanumero-password`, `slack-webhook-url`. The repo's `scripts/`
  directory has helpers (`scripts/ensure_aws_secrets.sh`) for setting these.
- **KOSKI account-side resources**: IAM role `kitu-sqs-sender` (with trust
  policy for the kitu Lambda role) and SQS queue
  `oma-opintopolku-loki-audit-queue`. Owned by the KOSKI team.
- **`TAG` env var** set to a tag that exists in the Util ECR repo. CI sets this
  automatically; locally use `TAG=$(git rev-parse main)` to deploy main's
  image, or pin to a specific commit.
