#!/usr/bin/env bash
set -euo pipefail

scripts_dir=$( dirname "${BASH_SOURCE[0]}" )
source "$scripts_dir/common-functions.sh"

require_command mise

# Trust the mise configuration
mise trust --quiet "$REPO_ROOT/.mise.toml"
# Enable experimental features (npm backend is experimental)
mise settings --quiet set experimental true

# Ensure mise is activated
if [ -z "${MISE_SHELL:-}" ]; then
  fatal "Mise does not seem to be activated. Run 'mise help activate' for activation instructions." \
        "Documentation is available at https://mise.jdx.dev"
fi

# Install dependencies
info "Installing dependencies..."
# Run install and auto-accept install prompts
mise install --quiet  --yes --cd="$REPO_ROOT"

require_command docker
require_command git

# exports secrets to environment variables
source "$scripts_dir/ensure_aws_secrets.sh"

# setup playwright
(cd "$REPO_ROOT"/e2e && npm i && npx playwright install)

### Additional options/switches ###

# --setup-only to skip creating tmux session
if [[ $* == *--setup-only* ]]; then
  readonly SETUP_ONLY="true"
else
  readonly SETUP_ONLY="false"
fi

kotorekisteri_start_tmux() {
  SESS_NAME=kotorekisteri

  (
    cd "$REPO_ROOT" || exit 1

    # Use old session if exists
    set +e
    tmux has-session -t $SESS_NAME 2>/dev/null
    HAS_SESSION="$?"
    set -e
    if [ "$HAS_SESSION" -eq "0" ]; then
      info "Attaching to existing tmux session..."
      tmux attach -t $SESS_NAME
      # TODO: Instead of return, kill the old session.
      return
   fi

    # If there is no session...
    # Start a new tmux session and detach immediately
    # Window 0:zsh
    info "Starting new tmux session..."
    tmux new-session -d -s $SESS_NAME

    # Window 0:database docker
    WINDOW="database"
    tmux rename-window -t $SESS_NAME:0 "$WINDOW"
    tmux send-keys -t $SESS_NAME:"$WINDOW.0" "$REPO_ROOT/scripts/start_database_docker.sh" C-m

    # Window 1:idea
    WINDOW="idea"
    tmux new-window -t $SESS_NAME -n "idea"
    tmux send-keys -t $SESS_NAME:"idea" "idea $REPO_ROOT" C-m

    # Window 2:springboot
    WINDOW="springboot"
    tmux new-window -t $SESS_NAME -n "$WINDOW"
    tmux send-keys -t $SESS_NAME:"$WINDOW" "$REPO_ROOT/scripts/start_local_server.sh" C-m

    # Window 3:workspace
    WINDOW="workspace"
    tmux new-window -t $SESS_NAME -n "$WINDOW"
    tmux send-keys -t $SESS_NAME:"$WINDOW" "cd $REPO_ROOT" C-m
    tmux send-keys -t $SESS_NAME:"$WINDOW" "git log --decorate=full --graph --all --oneline" C-m
    tmux split-window -h -t "${SESSION-}":"$WINDOW"
    tmux send-keys -t $SESS_NAME:"$WINDOW.1" "cd $REPO_ROOT" C-m
    tmux send-keys -t $SESS_NAME:"$WINDOW.1" "ls -la" C-m
    tmux send-keys -t $SESS_NAME:"$WINDOW.1" "git status" C-m

    tmux attach
  )
}

if [[ "$SETUP_ONLY" == "true" ]]; then
  info "Setup done."
  exit 0
fi

require_command tmux
kotorekisteri_start_tmux
