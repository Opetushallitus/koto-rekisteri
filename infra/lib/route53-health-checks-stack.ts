import { Stack, StackProps } from "aws-cdk-lib"
import { Metric } from "aws-cdk-lib/aws-cloudwatch"
import { SnsAction } from "aws-cdk-lib/aws-cloudwatch-actions"
import { CfnHealthCheck } from "aws-cdk-lib/aws-route53"
import { ITopic } from "aws-cdk-lib/aws-sns"
import { Construct } from "constructs"

export interface Route53HealthChecksProps extends StackProps {
  domainName: string
  alarmsSnsTopic: ITopic
}

export class Route53HealthChecksStack extends Stack {
  constructor(scope: Construct, id: string, props: Route53HealthChecksProps) {
    super(scope, id, props)

    const healthCheck = new CfnHealthCheck(this, "ServiceHealthCheck", {
      healthCheckConfig: {
        type: "HTTPS",
        fullyQualifiedDomainName: props.domainName,
      },
    })

    new Metric({
      metricName: "HealthCheckStatus",
      namespace: "AWS/Route53",
      dimensionsMap: {
        HealthCheckId: healthCheck.attrHealthCheckId,
      },
    })
      .createAlarm(this, "HealthCheckAlarm", {
        threshold: 1,
        evaluationPeriods: 1,
      })
      .addAlarmAction(new SnsAction(props.alarmsSnsTopic))
  }
}
