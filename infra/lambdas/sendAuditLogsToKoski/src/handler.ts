import type { APIGatewayProxyHandlerV2 } from "aws-lambda"

export const handler: APIGatewayProxyHandlerV2 = async (event) => {
  return {
    statusCode: 200,
    body: JSON.stringify({ ok: true, path: event.rawPath }),
  }
}
