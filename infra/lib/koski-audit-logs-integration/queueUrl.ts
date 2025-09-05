import { SSMClient, GetParameterCommand } from "@aws-sdk/client-ssm"

const Name = "/kitu/koski-integration/oma-opintopolku-loki-audit-queue"
const ssm = new SSMClient()
const command = new GetParameterCommand({ Name })
let queueUrl: string

export const getQueueUrl = async () => {
  if (!queueUrl) {
    const output = await ssm.send(command)

    if (!output.Parameter?.Value) {
      throw `Could not find parameter with param: '${Name}'.`
    }

    queueUrl = output.Parameter.Value
  }

  return queueUrl
}
