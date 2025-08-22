npm i
npm run build
npm run zip

aws lambda create-function \
  --function-name sendAuditLogsToKoski-ts \
  --runtime nodejs22.x \
  --role arn:aws:iam::682033502734:role/service-role/sendAuditLogsToKoski_test-role-er2ykcki \
  --handler index.handler \
  --zip-file fileb://function.zip