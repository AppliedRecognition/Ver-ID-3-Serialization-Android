pushd "$(dirname "$0")" > /dev/null
mkdir -p ./lib/src/main/java
protoc --java_out=./lib/src/main/java proto/**/*.proto
popd > /dev/null
