#!/bin/bash
set -e
cd "$(dirname "$0")/.."

ROUNDS=${ROUNDS:-500}
SEED=${SEED:-42}
EXTRA_ARGS=""

for arg in "$@"; do EXTRA_ARGS="$EXTRA_ARGS $arg"; done

mvn -q compile
mvn -q exec:java \
  -Dexec.mainClass=dev.bastienluben.algotresor.BenchmarkMain \
  -Dexec.args="--rounds $ROUNDS --seed $SEED$EXTRA_ARGS" \
  -Dexec.jvmArgs="-Xms256m -Xmx4g" \
  2>/dev/null
