import { CloudWatchLogsEvent } from "aws-lambda"
import { parse } from "./parser"

export const handler = async (event: CloudWatchLogsEvent) => {
  const { logEvents } = parse(event)
  // TODO: Send to SQS

  return {
    statusCode: 200,
    message: "ok",
  }
}
