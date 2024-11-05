import {
  GetSecretValueCommand,
  SecretsManagerClient,
} from "@aws-sdk/client-secrets-manager"
import type { CloudWatchAlarmEvent, CloudWatchAlarmHandler } from "aws-lambda"

const slackWebhookSecretName = process.env.SLACK_WEBHOOK_URL_SECRET_NAME

const secretsManagerClient = new SecretsManagerClient()
const secretResponse = await secretsManagerClient.send(
  new GetSecretValueCommand({
    SecretId: slackWebhookSecretName,
  }),
)

const slackWebhookUrl = secretResponse.SecretString!

async function sendSlackMessage(slackMessage: { text: string }) {
  const fetchResponse = await fetch(slackWebhookUrl, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(slackMessage),
  })

  return await fetchResponse.text()
}

export const handler: CloudWatchAlarmHandler = async (
  event: CloudWatchAlarmEvent,
) => {
  console.log("Received event:", JSON.stringify(event, null, 2))

  const {
    alarmData: { alarmName, state, previousState },
    time,
  } = event

  const date = new Date(time)

  const formattedDate = new Intl.DateTimeFormat("fi-FI", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    timeZone: "Europe/Helsinki",
    hour12: false,
  }).format(date)

  const getEmoji = () => {
    if (previousState.value === "ALARM" && state.value === "OK") {
      return ":sunny:"
    } else if (state.value === "ALARM") {
      return ":thunder_cloud_and_rain:"
    }
    return ":question:"
  }

  const slackMessage = {
    text: `
    *Alarm from CloudWatch* ${getEmoji()}
    *State changed*: \`${previousState.value}\` :arrow_right: \`${state.value}\`
    *Alarm Name*: ${alarmName}
    *Reason*: ${state.reason} ${state.reasonData ?? ""}
    *Timestamp*: ${formattedDate}
    `,
  }

  try {
    const responseBody = await sendSlackMessage(slackMessage)
    console.log(`Response from Slack: ${responseBody}`)
  } catch (error) {
    console.error(`Request failed: ${error}`)
    throw error
  }
}
