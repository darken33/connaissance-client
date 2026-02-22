#!/bin/bash
# Import custom jmeter path
JMETER_BIN_PATH="/home/pbousquet/Applications/apache-jmeter-5.4.1/bin"
# Var
RESULT_NAME=hello-api-tests.jtl
PATH_REPORT="$(echo "$RESULT_NAME" | cut -f 1 -d '.')_$(date +%FT%H:%M)"
echo "Generate report for test : $RESULT_NAME inside : $PATH_REPORT"

# Generate report
sh $JMETER_BIN_PATH/jmeter -e \
  -j ./logs/hello-api-tests.log \
  -g ./results/$RESULT_NAME \
  -o ./reports/$PATH_REPORT 
