#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAVA_CMD="${JAVA_HOME:-}/bin/java"

if [ ! -x "$JAVA_CMD" ]; then
  JAVA_CMD="java"
fi

exec "$JAVA_CMD" \
  --module-path "$SCRIPT_DIR/lib:$SCRIPT_DIR/@APP_JAR@" \
  --module @APP_MODULE@/@APP_MAIN_CLASS@ \
  "$@"
