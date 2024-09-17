#!/usr/bin/env bash
set -euo pipefail

source "$( dirname "${BASH_SOURCE[0]}" )/common-functions.sh"

require_command mise
require_command docker
require_command tmux
require_command git
require_command idea

# Ensure mise is activated
if [ -z "${MISE_SHELL:-}" ]; then
  echo "Mise does not seem to be activated. Run 'mise help activate' for activation instructions."
  echo "Documentation is available at https://mise.jdx.dev"
  exit 1
fi

# remember to chmod +x start_local_env.sh
kotorekisteri_start_tmux() {
  SESS_NAME=kotorekisteri
  PANE1=kotorekisteri
  REPO_ROOT=${1:-'.'}

  (
    cd $REPO_ROOT

    # Install dependencies
    echo "Installing dependencies..."
    mise install

    # Use old session if exists
    set +e
    tmux has-session -t $SESS_NAME 2>/dev/null
    HAS_SESSION="$?"
    set -e
    if [ "$HAS_SESSION" -eq "0" ]; then
      tmux attach -t $SESS_NAME
      # TODO: Instead of return, kill the old session.
      return
   fi

    # If there is no session...
    # Start a new tmux session and detach immediately
    # Window 0:zsh
    echo "Starting new tmux session..."
    tmux new-session -d -s $SESS_NAME

    # Window 0:database
    WINDOW="database"
    # database: left pane (docker running)
    tmux rename-window -t $SESS_NAME:0 "$WINDOW"
    tmux send-keys -t $SESS_NAME:"$WINDOW.0" "./scripts/start_database_docker.sh" C-m

    # database: right pane (flyway migrate)
    tmux split-window -h -t "${SESSION-}":"$WINDOW"
    tmux send-keys -t $SESS_NAME:"$WINDOW.1" "(cd server &&
      sleep 5 && echo \"10 seconds left to run migrations...\" &&
      sleep 5 && echo \"05 seconds left to run migrations...\" &&
      ./mvnw flyway:migrate)" C-m

    # Window 1:idea
    WINDOW="idea"
    tmux new-window -t $SESS_NAME -n "idea"
    tmux send-keys -t $SESS_NAME:"idea" "idea ." C-m

    # Window 2:springboot
    WINDOW="springboot"
    tmux new-window -t $SESS_NAME -n "$WINDOW"
    tmux send-keys -t $SESS_NAME:"$WINDOW" "cd server && sleep 5" C-m
    tmux send-keys -t $SESS_NAME:"$WINDOW" "mvn clean install" C-m
    tmux send-keys -t $SESS_NAME:"$WINDOW" "mvn package" C-m
    tmux send-keys -t $SESS_NAME:"$WINDOW" "./mvnw spring-boot:run" C-m

    # Window 3:workspace
    WINDOW="workspace"
    tmux new-window -t $SESS_NAME -n "$WINDOW"
    tmux send-keys -t $SESS_NAME:"$WINDOW" "git log --oneline --decorate=full --graph --all --oneline" C-m
    tmux split-window -h -t "${SESSION-}":"$WINDOW"
    tmux send-keys -t $SESS_NAME:"$WINDOW.1" "ls -la" C-m
    tmux send-keys -t $SESS_NAME:"$WINDOW.1" "git status" C-m

    tmux attach
  )
}

kotorekisteri_start_tmux "${1-}"
