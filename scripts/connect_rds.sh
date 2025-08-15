#!/usr/bin/env bash

set -euo pipefail

scripts_dir=$( dirname "${BASH_SOURCE[0]}" )
source "$scripts_dir/common-functions.sh"

env=Dev

aws() {
  command aws --output text "$@"
}

quiet() {
  "$@" >/dev/null
}

get_stack_output() {
  local stack=$1
  local output=$2
  aws cloudformation describe-stacks --stack-name "$env-$stack" --query "Stacks[0].Outputs[?starts_with(OutputKey, '$output')].OutputValue[0]"
}

start_task() {
  local cluster=$1
  local task_def=$2
  info "Starting temporary ECS proxy task..."
  aws ecs run-task --cluster "$cluster" --task_definition "$task_def" --query 'tasks[0].taskArn'
}

wait_for_task_start() {
  local cluster=$1
  local task_arn=$2
  info "Waiting for task to report running..."
  quiet aws ecs wait tasks-running --cluster "$cluster" --tasks "$task_arn"
  aws ecs describe-tasks --cluster "$cluster" --tasks "$task_arn" --query "tasks[0].containers[0].runtimeId"
}

establish_session() {
  local cluster=$1
  local container=$2
  local host=$3
  info "Connecting to proxy task..."
  aws ssm start-session \
    --target "ecs:$cluster:$container" \
    --document-name AWS-StartPortForwardingSessionToRemoteHost \
    --parameters "host=$host,portNumber=5432,localPortNumber=8432"
}

stop_task() {
  local cluster=$1
  local task_arn=$2
  aws ecs stop-task --cluster "$cluster" --task "$task_arn"
}

cluster_name=$(get_stack_output Service ClusterName)
task_definition=$(get_stack_output EcsRdsProxy TaskDefinitionArn)
database_hostname=$(get_stack_output Database EndpointROHost)

task_arn=$(start_task "$cluster_name" "$task_definition")

trap 'stop_task $cluster_name $task_arn' EXIT

container_runtime_id=$(wait_for_task_start "$cluster_name" "$task_arn")

establish_session "$cluster" "$container_runtime_id" "$database_hostname"
