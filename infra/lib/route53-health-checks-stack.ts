import { Stack, StackProps } from "aws-cdk-lib"
import { ComparisonOperator, Metric } from "aws-cdk-lib/aws-cloudwatch"
import { LambdaAction, SnsAction } from "aws-cdk-lib/aws-cloudwatch-actions"
import { CfnHealthCheck } from "aws-cdk-lib/aws-route53"
import { ITopic } from "aws-cdk-lib/aws-sns"
import { Construct } from "constructs"
import { IFunction } from "aws-cdk-lib/aws-lambda"

export interface Route53HealthChecksProps extends StackProps {
  domainName: string
  alarmLambdaHandler: IFunction
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

    const alarm = new Metric({
      metricName: "HealthCheckStatus",
      namespace: "AWS/Route53",
      dimensionsMap: {
        HealthCheckId: healthCheck.attrHealthCheckId,
      },
    }).createAlarm(this, "HealthCheckAlarm", {
      comparisonOperator: ComparisonOperator.LESS_THAN_THRESHOLD,
      threshold: 1, // 1 is healthy, 0 is unhealthy
      evaluationPeriods: 1,
    })
    const lambdaAction = new LambdaAction(props.alarmLambdaHandler)
    alarm.addAlarmAction(lambdaAction)
    alarm.addOkAction(lambdaAction)
  }
}
