#!/usr/bin/env bash

LAMBDA_NAME="sendAuditLogsToKoski"

kotlinc src/main/kotlin \
  -include-runtime \
  -cp lib/aws-lambda-java-core-1.3.0.jar \
  -d "$LAMBDA_NAME.jar"

zip "$LAMBDA_NAME.zip" \
  "$LAMBDA_NAME.jar" \
  libs/aws-lambda-java-core-1.2.3.jar

# Uploads to dev
aws lambda create-function \
  --function-name "$LAMBDA_NAME-kt-test" \
  --runtime java21 \
  --memory-size 512 \
  --timeout 10 \
  --handler fi.oph.kitu.Handler::handleRequest \
  --role arn:aws:iam::682033502734:role/sendAuditLogsToKoski_test-role-er2ykcki \
  --zip-file fileb://"$LAMBDA_NAME.zip"
