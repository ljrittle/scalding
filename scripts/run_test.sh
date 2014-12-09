# Identify the bin dir in the distribution, and source the common include script
BASE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )"/.. && pwd )"
cd $BASE_DIR

withCmd() {
  CMD=$1
  for t in $TEST_TARGET; do echo "$t/$CMD"; done
}

bash -c "while true; do echo -n .; sleep 5; done" &
PROGRESS_REPORTER_PID=$!
time ./sbt ++$TRAVIS_SCALA_VERSION $(withCmd compile) $(withCmd test:compile) &> /dev/null
kill -9 $PROGRESS_REPORTER_PID

export JVM_OPTS="-XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC -XX:ReservedCodeCacheSize=96m -XX:+TieredCompilation -XX:MaxPermSize=128m -Xms256m -Xmx512m -Xss2m"

./sbt -Dlog4j.configuration=file://$TRAVIS_BUILD_DIR/project/travis-log4j.properties ++$TRAVIS_SCALA_VERSION $(withCmd test)
