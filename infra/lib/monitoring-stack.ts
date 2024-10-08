import * as cdk from "aws-cdk-lib"
import { ApplicationLoadBalancedFargateService } from "aws-cdk-lib/aws-ecs-patterns"
import { HttpCodeTarget } from "aws-cdk-lib/aws-elasticloadbalancingv2"
import { ILogGroup } from "aws-cdk-lib/aws-logs"
import { IDatabaseCluster } from "aws-cdk-lib/aws-rds"
import { ITopic } from "aws-cdk-lib/aws-sns"
import {
  MonitoringFacade,
  SnsAlarmActionStrategy,
} from "cdk-monitoring-constructs"
import { Construct } from "constructs"

interface MonitoringStackProps extends cdk.StackProps {
  serviceLogGroup: ILogGroup
  alarmNamePrefix: string
  alarmSnsTopic: ITopic
  databaseCluster: IDatabaseCluster
  service: ApplicationLoadBalancedFargateService
}

export class MonitoringStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: MonitoringStackProps) {
    super(scope, id, props)

    const facade = new MonitoringFacade(this, "Facade", {
      alarmFactoryDefaults: {
        action: new SnsAlarmActionStrategy({
          onAlarmTopic: props.alarmSnsTopic,
          onOkTopic: props.alarmSnsTopic,
        }),
        actionsEnabled: true,
        alarmNamePrefix: props.alarmNamePrefix,
      },
    })

    facade
      .monitorLog({
        logGroupName: props.serviceLogGroup.logGroupName,
        alarmFriendlyName: "Service-Logs-Errors",
        pattern: "ERROR",
      })
      .monitorLog({
        logGroupName: props.serviceLogGroup.logGroupName,
        alarmFriendlyName: "Service-Logs-Blobo",
        addMinIncomingLogsAlarm: { Warning: { minCount: 1 } },
      })
      .monitorAuroraCluster({
        addCpuUsageAlarm: { Warning: { maxUsagePercent: 50 } },
        cluster: props.databaseCluster,
        alarmFriendlyName: "Koto Database Alarm",
      })
      .monitorFargateApplicationLoadBalancer({
        addUnhealthyTaskCountAlarm: { Warning: { maxUnhealthyTasks: 1 } },
        addMemoryUsageAlarm: { Warning: { maxUsagePercent: 50 } },
        addCpuUsageAlarm: { Warning: { maxUsagePercent: 50 } },
        addEphermalStorageUsageAlarm: { Warning: { maxUsagePercent: 50 } },
        fargateService: props.service.service,
        applicationLoadBalancer: props.service.loadBalancer,
        applicationTargetGroup: props.service.targetGroup,
      })
      .monitorCustom({
        alarmFriendlyName: "Service-HTTP5xx-Responses",
        metricGroups: [
          {
            title: "Target HTTP count",
            metrics: [
              props.service.loadBalancer.metrics.httpCodeTarget(
                HttpCodeTarget.TARGET_5XX_COUNT,
              ),
            ],
          },
        ],
      })
  }
}
