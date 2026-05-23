#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAVA_CMD="${JAVA_HOME:-}/bin/java"
APP_MODULE="com.yangdesigner.yangmodeldesigner"
APP_MAIN_CLASS="com.yangdesigner.yangmodeldesigner.app.YangModelDesignerApplication"

if [ ! -x "$JAVA_CMD" ]; then
  JAVA_CMD="java"
fi

APP_JAR="${APP_JAR:-}"
if [ -z "$APP_JAR" ]; then
  for candidate in "$SCRIPT_DIR"/YangModelDesigner-*.jar; do
    if [ -f "$candidate" ]; then
      APP_JAR=$(basename "$candidate")
      break
    fi
  done
fi

if [ -z "$APP_JAR" ] || [ ! -f "$SCRIPT_DIR/$APP_JAR" ]; then
  echo "YangModelDesigner jar not found next to run.sh" >&2
  exit 1
fi

exec "$JAVA_CMD" \
  --module-path "$SCRIPT_DIR/lib:$SCRIPT_DIR/$APP_JAR" \
  --module "$APP_MODULE/$APP_MAIN_CLASS" \
  "$@"
