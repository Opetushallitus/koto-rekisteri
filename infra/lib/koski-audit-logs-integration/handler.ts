import {
  BatchResultErrorEntry,
  SendMessageBatchCommand,
  SendMessageBatchRequestEntry,
  SQSClient,
} from "@aws-sdk/client-sqs"
import { fromTemporaryCredentials } from "@aws-sdk/credential-providers"
import { CloudWatchLogsEvent } from "aws-lambda"
import { getAuditLogEntry, parse } from "./parser"

const SQS_BATCH_SIZE = 10

export const handler = async (event: CloudWatchLogsEvent) => {
  const QueueUrl = process.env.KOSKI_SQS_QUEUE_URL
  if (!QueueUrl) {
    throw "Cannot proceed, because 'KOSKI_SQS_QUEUE_URL' is missing."
  }

  const roleArn = process.env.KOSKI_ROLE_ARN
  if (!roleArn) {
    throw "Cannot proceed, because 'KOSKI_ROLE_ARN' is missing."
  }

  const data = parse(event)
  const auditLogEntries = getAuditLogEntry(data)

  if (auditLogEntries.length === 0) {
    return { statusCode: 200, message: "no audit log entries to send" }
  }

  const sqs = new SQSClient({
    credentials: fromTemporaryCredentials({
      params: { RoleArn: roleArn },
    }),
  })

  const entries: SendMessageBatchRequestEntry[] = auditLogEntries.map(
    (entry, index) => ({
      Id: String(index),
      MessageBody: JSON.stringify(entry),
    }),
  )

  const failed: BatchResultErrorEntry[] = []
  for (let i = 0; i < entries.length; i += SQS_BATCH_SIZE) {
    const Entries = entries.slice(i, i + SQS_BATCH_SIZE)
    try {
      const result = await sqs.send(
        new SendMessageBatchCommand({ QueueUrl, Entries }),
      )
      if (result.Failed?.length) {
        failed.push(...result.Failed)
      }
    } catch (error) {
      console.log("failed to send batch to sqs", error)
      return {
        statusCode: 500,
        message: "unknown error",
        error,
      }
    }
  }

  if (failed.length) {
    // Return non-2xx so the CloudWatch Logs subscription retries the whole event.
    // Audit logs tolerate duplicates better than data loss.
    console.log("partial failure sending audit logs to sqs", failed)
    return {
      statusCode: 500,
      message: "partial failure",
      failed,
    }
  }

  console.log(`sent ${auditLogEntries.length} audit log entries to sqs`)
  return {
    statusCode: 200,
    message: "ok",
    sent: auditLogEntries.length,
  }
}
