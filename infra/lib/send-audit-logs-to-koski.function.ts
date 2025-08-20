import { gunzipSync } from "node:zlib"

interface AuditLogsSubscriptFilterEvent {
  awslogs: {
    data: string
  }
}

export const handler = async (event: AuditLogsSubscriptFilterEvent) => {
  const payload = Buffer.from(event.awslogs.data, "base64")
  const decompressed = gunzipSync(payload)
  const { logEvents } = JSON.parse(decompressed.toString())

  // TODO: Send to SQS

  return {
    statusCode: 200,
    message: "ok",
  }
}
