import { Stack, StackProps } from "aws-cdk-lib"
import { Construct } from "constructs"
import {
  BackupPlan,
  BackupResource,
  BackupVault,
  BackupVaultEvents,
} from "aws-cdk-lib/aws-backup"
import { ITopic } from "aws-cdk-lib/aws-sns"

export interface BackupsStackProps extends StackProps {
  notificationTopic: ITopic
  resources: BackupResource[]
}

export class BackupsStack extends Stack {
  readonly backupPlan: BackupPlan

  constructor(scope: Construct, id: string, props: BackupsStackProps) {
    super(scope, id, props)

    this.backupPlan = BackupPlan.dailyWeeklyMonthly5YearRetention(
      this,
      "BackupPlan",
      new BackupVault(this, "BackupVault", {
        notificationTopic: props.notificationTopic,
        // All the failure events documented here: https://docs.aws.amazon.com/aws-backup/latest/devguide/API_PutBackupVaultNotifications.html#Backup-PutBackupVaultNotifications-request-BackupVaultEvents
        notificationEvents: [
          BackupVaultEvents.BACKUP_JOB_COMPLETED,
          BackupVaultEvents.COPY_JOB_FAILED,
          BackupVaultEvents.RESTORE_JOB_COMPLETED,
          BackupVaultEvents.S3_BACKUP_OBJECT_FAILED,
          BackupVaultEvents.S3_RESTORE_OBJECT_FAILED,
        ],
      }),
    )

    this.backupPlan.addSelection("Selection", {
      resources: props.resources,
    })
  }
}
