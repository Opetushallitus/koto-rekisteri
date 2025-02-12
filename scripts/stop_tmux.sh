#!/usr/bin/env bash

# Check if the script is running inside a tmux session
if [[ -z "$TMUX" ]]; then
  echo "Script not running inside a tmux session" >&2
  exit 1
fi

# Iterate over current session windows and panes
tmux list-windows -F '#{window_index}' | while read -r window; do
  tmux list-panes -t "$window" -F '#{pane_index}' | while read -r pane; do
    tmux send-keys -t "$window.$pane" C-c
  done
done

echo "Waiting 5 seconds before killing the session"
sleep 5

tmux kill-session
