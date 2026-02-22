#!/bin/bash
# custom jmeter path
JMETER_BIN_PATH="/home/pbousquet/Applications/apache-jmeter-5.4.1/bin"
# Var
TEMPLATE_TEST_NAME=connaissance-client-tests.jmx
BASE_NAME=$(echo "$TEMPLATE_TEST_NAME" | cut -f 1 -d '.')
#CONF_NAME="$BASE_NAME.properties"
RESULT_NAME="$BASE_NAME.jtl"
LOG_NAME="$BASE_NAME.log"
echo "---------------------------------------------"
echo "* Testing : $TEMPLATE_TEST_NAME"
#echo "* conf : $CONF_NAME"
echo "* log : $LOG_NAME"
echo "* result : $RESULT_NAME"
echo "---------------------------------------------"

#      -q ../conf/Gigya-authentication.properties \
#      -q ../conf/ADFS-authentication.properties \
#      -q ../conf/APIM-subscriptions-keys.properties \
# 			-p ../conf/$CONF_NAME \

sh $JMETER_BIN_PATH/jmeter -n \
 			-t ./$TEMPLATE_TEST_NAME \
 			-l ./results/$RESULT_NAME \
 			-j ./logs/$LOG_NAME 

