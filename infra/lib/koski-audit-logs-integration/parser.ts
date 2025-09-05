import { CloudWatchLogsEvent } from "aws-lambda"
import { gunzipSync } from "zlib"

export const parse = (event: CloudWatchLogsEvent): CloudWatchData => {
  const decoded = Buffer.from(event.awslogs.data, "base64")
  const decompressed = gunzipSync(decoded)
  return JSON.parse(decompressed.toString())
}

export const getAuditLogEntry = (data: CloudWatchData): AuditLogEntry[] => {
  return data.logEvents.map((logEvent) => {
    const kituLogEvent = JSON.parse(logEvent.message) as KituLogEvent
    return JSON.parse(kituLogEvent.message) as AuditLogEntry
  })
}
