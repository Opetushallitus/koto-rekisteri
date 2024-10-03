#!/usr/bin/env bash

set -eux

cluster=$(aws ecs list-clusters --output text --query clusterArns)
task_definitions=$(aws ecs list-task-definitions --output text --query taskDefinitionArns)
log_group=$(aws ecs describe-task-definition --task-definition $task_definitions --output text --query 'taskDefinition.containerDefinitions[*].logConfiguration.options."awslogs-group"')
log_group_arn=$(aws logs describe-log-groups --log-group-name-prefix $log_group --query 'logGroups[*].logGroupArn' --output text)
aws logs start-live-tail --log-group-identifiers $log_group_arn
