#!/bin/bash
set -e
cd "$(dirname "$0")/.."
mvn -q package -DskipTests
java -jar lib/voleurs_de_tresors.jar \
  -player1 "java -cp target/algo-tresor-1.0-SNAPSHOT.jar dev.bastienluben.algotresor.Main"
