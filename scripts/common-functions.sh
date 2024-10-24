#!/usr/bin/env bash
set -euo pipefail

# allow sourcing this file multiple times from different scripts
if [ -n "${COMMON_FUNCTIONS_SOURCED:-}" ]; then
  return
fi
readonly COMMON_FUNCTIONS_SOURCED="true"

REPO_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )" && readonly REPO_ROOT
export REPO_ROOT

function require_command {
  if ! command -v "$1" > /dev/null; then
    fatal "$1 is required, but it's not installed. Aborting."
  fi
}

function require_env {
  if [ -z "$(set +e; printenv "$1")" ]; then
    fatal "Environment variable $1 is required, but it is empty or not defined. Aborting."
  fi
}

function info {
  log "INFO" "$*"
}

function fatal {
  log "ERROR" "$*"
  exit 1
}

function log {
  local -r level="$1"
  local -r message="${*:2}"
  local -r timestamp=$(date +"%Y-%m-%d %H:%M:%S")

  >&2 echo -e "${timestamp} ${level} ${message}"
}

function dedent {
  sed 's/^[[:space:]]*//'
}

function require_dev_aws_session {
  info "Verifying that AWS session has not expired"
  ## SSO Login does not work in container
  aws sts get-caller-identity --profile=oph-ktr-dev 1>/dev/null || {
    info "Session is expired"
    aws --profile oph-ktr-dev sso login
  }
  AWS_PROFILE=oph-ktr-dev; export AWS_PROFILE
}

function ensure_aws_profile {
  local name=$1
  local account_id=$2
  aws configure set --profile "$name" sso_session oph-session
  aws configure set --profile "$name" sso_account_id "$account_id"
  aws configure set --profile "$name" region eu-west-1
  aws configure set --profile "$name" output yaml
  aws configure set --profile "$name" sso_role_name AdministratorAccess
}

function ensure_aws_sso_session {
  # I couldn't find a CLI command for SSO sessions like ensure_aws_profile uses for profiles.
  grep -F "[sso-session oph-session]" ~/.aws/config 1>/dev/null || {
    echo "
      [sso-session oph-session]
      sso_start_url = https://oph-aws-sso.awsapps.com/start
      sso_region = eu-west-1
      sso_registration_scopes = sso:account:access
    " | dedent >> ~/.aws/config
  }
}

function configure_aws_sso_profiles {
  mkdir -p ~/.aws

  ensure_aws_sso_session

  ensure_aws_profile oph-ktr-util 961341524988
  ensure_aws_profile oph-ktr-dev 682033502734
  ensure_aws_profile oph-ktr-test 961341546901
  ensure_aws_profile oph-ktr-prod 515966535475
}
