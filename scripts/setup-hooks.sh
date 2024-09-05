#!/bin/sh

set -e

echo "Setting up pre-commit hook..."

cp ./hooks/pre-commit ./.git/hooks/pre-commit
chmod +x ./.git/hooks/pre-commit

echo "Pre-commit hook setup completed."