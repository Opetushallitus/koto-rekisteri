import {
  SQSClient,
  SendMessageCommand,
  SendMessageCommandInput,
} from "@aws-sdk/client-sqs"
import { gunzipSync } from "zlib"

// Action: sqs:SendMessage
const QUEUE_URL = process.env.QUEUE_URL!

// TODO: Should region be hardcoded?
const sqs = new SQSClient({ region: "eu-west-1" })

// TODO: Is there a built-in type for CloudWatchLogsEvent?
interface CloudWatchLogsEvent {
  awslogs: {
    data: string // Base64 + GZIP compressed payload
  }
}

interface DecodedLogEvent {
  logEvents: {
    id: string
    timestamp: string
    message: string
  }[]
  logGroup: string
  logStream: string
}

export const handler = async (event: CloudWatchLogsEvent) => {
  const unzipped_json = Buffer.from(event.awslogs.data, "base64")
  const json_str = gunzipSync(unzipped_json).toString("utf-8")
  const decoded: DecodedLogEvent = JSON.parse(json_str)

  let forwarded = 0

  for (const e of decoded.logEvents) {
    // TODO: Apply filtering logic here

    const body = JSON.stringify({
      id: e.id,
      timestamp: e.timestamp,
      message: e.message,
      logGroup: decoded.logGroup,
      logStream: decoded.logStream,
    })

    const cmdInput: SendMessageCommandInput = {
      QueueUrl: QUEUE_URL,
      MessageBody: body,
    }

    // TODO: Check if destination queue is FIFO (add MessageGroupId, DeduplicationId if needed)
    await sqs.send(new SendMessageCommand(cmdInput))
    forwarded++
  }

  return { forwarded }
}
