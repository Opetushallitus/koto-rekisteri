import {
  GetSecretValueCommand,
  SecretsManagerClient,
} from "@aws-sdk/client-secrets-manager"
import { Handler, SNSEvent } from "aws-lambda"

const secretName = process.env.SLACK_WEBHOOK_URL_SECRET_NAME

const client = new SecretsManagerClient()
const response = await client.send(
  new GetSecretValueCommand({
    SecretId: secretName,
  }),
)

const slackWebhookUrl = response.SecretString!

export const handler: Handler = async (event: SNSEvent) => {
  console.log("Received event:", JSON.stringify(event, null, 2))

  const message = event.Records[0].Sns.Message

  const {
    AlarmName,
    NewStateValue,
    NewStateReason,
    OldStateValue,
    StateChangeTime,
  } = JSON.parse(message)

  const date = new Date(StateChangeTime)

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
    if (OldStateValue === "ALARM" && NewStateValue === "OK") {
      return ":sunny:"
    } else if (NewStateValue === "ALARM") {
      return ":thunder_cloud_and_rain:"
    }
    return ":question:"
  }

  const slackMessage = {
    text: `
    *Alarm from CloudWatch* ${getEmoji()}
    *State changed*: \`${OldStateValue}\` :arrow_right: \`${NewStateValue}\`
    *Alarm Name*: ${AlarmName}
    *Reason*: ${NewStateReason}
    *Timestamp*: ${formattedDate}
    `,
  }

  try {
    const fetchResponse = await fetch(slackWebhookUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(slackMessage),
    })

    const responseBody = await fetchResponse.text()
    console.log(`Response from Slack: ${responseBody}`)
  } catch (error) {
    console.error(`Request failed: ${error}`)
    throw error
  }
}
