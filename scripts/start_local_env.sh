#!/usr/bin/env bash
set -euo pipefail

source "$( dirname "${BASH_SOURCE[0]}" )/common-functions.sh"

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

### Additional options/switches ###

# --setup-only to skip creating tmux session
if [[ $* == *--setup-only* ]]; then
  readonly SETUP_ONLY="true"
else
  readonly SETUP_ONLY="false"
fi

kotorekisteri_start_tmux() {
  SESS_NAME=kotorekisteri
  PANE1=kotorekisteri

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

    # Window 0:database
    WINDOW="database"
    # database: left pane (docker running)
    tmux rename-window -t $SESS_NAME:0 "$WINDOW"
    tmux send-keys -t $SESS_NAME:"$WINDOW.0" "$REPO_ROOT/scripts/start_database_docker.sh" C-m

    # database: right pane (flyway migrate)
    tmux split-window -h -t "${SESSION-}":"$WINDOW"
    tmux send-keys -t $SESS_NAME:"$WINDOW.1" "(cd $REPO_ROOT/server &&
      echo \"Waiting 10 seconds before running migrations...\" && sleep 5 &&
      echo \"Waiting 05 seconds before running migrations...\" && sleep 5 &&
      ./mvnw flyway:migrate)" C-m

    # Window 1:idea
    WINDOW="idea"
    tmux new-window -t $SESS_NAME -n "idea"
    tmux send-keys -t $SESS_NAME:"idea" "idea $REPO_ROOT" C-m

    # Window 2:springboot
    WINDOW="springboot"
    tmux new-window -t $SESS_NAME -n "$WINDOW"
    tmux send-keys -t $SESS_NAME:"$WINDOW" "cd $REPO_ROOT/server && sleep 5" C-m
    tmux send-keys -t $SESS_NAME:"$WINDOW" "./mvnw clean spring-boot:run" C-m

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
