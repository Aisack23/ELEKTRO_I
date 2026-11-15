#!/usr/bin/env bash
# Compatibility entry point; Maven is the source of truth.
set -euo pipefail
cd "$(dirname "$0")/.."
exec ./mvnw clean verify "$@"
