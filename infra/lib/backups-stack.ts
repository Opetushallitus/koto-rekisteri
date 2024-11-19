import { Stack, StackProps } from "aws-cdk-lib"
import { Construct } from "constructs"
import { BackupPlan, BackupResource } from "aws-cdk-lib/aws-backup"

export interface BackupsStackProps extends StackProps {
  resources: BackupResource[]
}

export class BackupsStack extends Stack {
  readonly backupPlan = BackupPlan.dailyWeeklyMonthly5YearRetention(
    this,
    "BackupPlan",
  )

  constructor(scope: Construct, id: string, props: BackupsStackProps) {
    super(scope, id, props)

    this.backupPlan.addSelection("Selection", {
      resources: props.resources,
    })
  }
}
