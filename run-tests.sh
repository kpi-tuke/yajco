#!/bin/bash
set -ex  # Stop on first failure, trace commands
EXAMPLES_DIR=tmp-yajco-examples

mvn install --fail-at-end

# Find current yajco version.
# Based on https://blog.soebes.de/blog/2018/06/09/help-plugin/
# maven-help-plugin cannot be used because of issue 154:
# https://issues.apache.org/jira/browse/MPH-154
YAJCO_VERSION=$(mvn -q \
    -Dexec.executable="echo" \
    -Dexec.args='${project.version}' \
    --non-recursive \
    org.codehaus.mojo:exec-maven-plugin:1.6.0:exec)

# Clone yajco-examples and execute tests
git clone --depth=1 https://github.com/kpi-tuke/yajco-examples $EXAMPLES_DIR
cd $EXAMPLES_DIR
mvn verify --fail-at-end -Dyajco.version=$YAJCO_VERSION
cd .. && rm -rf $EXAMPLES_DIR
