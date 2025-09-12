#!/usr/bin/env bash

set -euo pipefail

scripts_dir=$( dirname "${BASH_SOURCE[0]}" )
source "$scripts_dir/common-functions.sh"

run() {
  [[ $# != 1 ]] && {
    echo "usage: $0 <Dev|Test|Prod>"
    exit 1
  }

  local env=$1

  [[ $env != Dev && $env != Test && $env != Prod ]] && {
    fatal "env must be one of: Dev, Test, Prod"
  }

  local cluster_name task_definition database_hostname

  info "Getting environment information"

  cluster_name=$(get_stack_output "$env" Service ClusterName)
  task_definition=$(get_stack_output "$env" EcsRdsProxy TaskDefinitionArn)
  execution_role_arn=$(get_stack_output "$env" EcsRdsProxy TaskExecutionRoleArn)
  database_hostname=$(get_stack_output "$env" Database EndpointROHost)
  private_subnets=$(get_subnets "$env" Private)
  security_groups=$(get_stack_output "$env" EcsRdsProxy SecurityGroup)

  info "Starting temporary ECS proxy task"
  task_arn=$(start_task "$cluster_name" "$task_definition" "$execution_role_arn" "$private_subnets" "$security_groups")

  trap 'stop_task "$cluster_name" "$task_arn"' EXIT

  info "Waiting for task to report running"
  container_runtime_id=$(wait_for_task_start "$cluster_name" "$task_arn")

  info "Connecting to proxy task"
  establish_session "$cluster_name" "$container_runtime_id" "$database_hostname"

  read -rp "Press any key to quit..."
}

aws() {
  command aws --output text "$@"
}

quiet() {
  "$@" >/dev/null
}

get_subnets() {
  local env=$1
  local kind=$2

  aws ec2 describe-subnets --filters Name=tag:aws-cdk:subnet-type,Values="$kind" --query 'Subnets[].SubnetId' --output text | tr \\t ,
}

get_stack_output() {
  local env=$1
  local stack=$2
  local output=$3
  local value
  # use starts_with here instead of equality comparison because CDK adds a hash suffix to output names
  value=$(aws cloudformation describe-stacks --stack-name "$env-$stack" --query "Stacks[0].Outputs[?starts_with(OutputKey, '$output')] | [0].OutputValue")
  [[ -z $value ]] && fatal "Could not find stack output stack=$env-$stack output=$output"
  echo "$value"
}

start_task() {
  local cluster=$1
  local task_def=$2
  local execution_role_arn=$3
  local subnets=$4
  local security_groups=$5
  aws ecs run-task --cluster "$cluster" --task-definition "$task_def" \
    --network-configuration "awsvpcConfiguration={subnets=[$subnets],securityGroups=[$security_groups],assignPublicIp=DISABLED}" \
    --overrides '{"executionRoleArn":"'"$execution_role_arn"'","containerOverrides": [{"name":"proxy","command":["sleep", "60m"]}]}' \
    --enable-execute-command \
    --launch-type FARGATE \
    --query 'tasks[0].taskArn'
}

wait_for_task_start() {
  local cluster=$1
  local task_arn=$2
  quiet aws ecs wait tasks-running --cluster "$cluster" --tasks "$task_arn"
  aws ecs describe-tasks --cluster "$cluster" --tasks "$task_arn" --query "tasks[0].containers[0].runtimeId"
}

establish_session() {
  local cluster=$1
  local container_id=$2
  local host=$3
  aws ssm start-session \
    --target "ecs:${cluster}_proxy_${container_id}" \
    --document-name AWS-StartPortForwardingSessionToRemoteHost \
    --parameters "host=$host,portNumber=5432,localPortNumber=8432"
}

stop_task() {
  local cluster=$1
  local task_arn=$2
  info "Stopping proxy task"
  aws ecs stop-task --cluster "$cluster" --task "$task_arn"
}

run "$@"
