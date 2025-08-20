// index.mjs
import { gunzipSync } from "node:zlib"

// TODO
// 1. Subscription filter - ehkä ok?
// 2. tämä lambda
// -> sqs
// 3. konffaa koski
const getStream = (data) => {
  const payload = Buffer.from(data, "base64")
  const decompressed = gunzipSync(payload)

  return JSON.parse(decompressed.toString())
}

export const handler = async (event, context) => {
  console.log("deployed via CDK")
  console.log("huh huh hui")
  const { logEvents } = getStream(event.awslogs.data)

  console.log(logEvents)
  console.log("done didi done")

  return {
    statusCode: 200,
    foo: "bar",
    body: logEvents,
  }
}
