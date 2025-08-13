import { Stack, StackProps } from "aws-cdk-lib"
import { IDatabaseCluster } from "aws-cdk-lib/aws-rds"
import {
  AmazonLinuxCpuType,
  BastionHostLinux,
  InstanceClass,
  InstanceSize,
  InstanceType,
  IVpc,
  MachineImage,
  Port,
} from "aws-cdk-lib/aws-ec2"
import { Construct } from "constructs"

export interface BastionStackProps extends StackProps {
  vpc: IVpc
  cluster: IDatabaseCluster
}

export class BastionStack extends Stack {
  private readonly bastion: BastionHostLinux

  constructor(scope: Construct, id: string, props: BastionStackProps) {
    super(scope, id, props)

    this.bastion = new BastionHostLinux(this, "Bastion", {
      vpc: props.vpc,
      machineImage: MachineImage.latestAmazonLinux2023({
        cpuType: AmazonLinuxCpuType.ARM_64,
      }),
      instanceType: InstanceType.of(
        InstanceClass.BURSTABLE4_GRAVITON,
        InstanceSize.NANO,
      ),
    })

    props.cluster.connections.allowFrom(this.bastion, Port.tcp(5432))
  }
}
