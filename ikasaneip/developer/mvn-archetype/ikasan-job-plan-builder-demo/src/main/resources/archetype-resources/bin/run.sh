#!/bin/bash
#set -u

SCRIPT_DIR=$(pwd)

# JVM Settings
MODULE_NAME=`cat config/application.properties|grep "module.name"|head -1|cut -d'=' -f2`
MODULE_JVM_OPTS="-Xms256m -Xmx256m -XX:MaxMetaspaceSize=96m -Dorg.apache.activemq.SERIALIZABLE_PACKAGES=*"
MODULE_OTHER_OPTS=""
MODULE_JAVA_OPTS="$MODULE_JVM_OPTS  $MODULE_OTHER_OPTS"

BUILD_JOB_PLAN_BUNDLE="BUILD_JOB_PLAN_BUNDLE"
BUILD_AND_DEPLOY_JOB_PLAN_BUNDLE="BUILD_AND_DEPLOY_JOB_PLAN_BUNDLE"
DEPLOY_JOB_PLAN_BUNDLE="DEPLOY_JOB_PLAN_BUNDLE"

APPLICATION_JAR=${MODULE_NAME}*.jar

JAVA=$JAVA_HOME/bin/java

cd $SCRIPT_DIR
mkdir -p logs

FILE_TO_DEPLOY=$2

# Prints command usage.
function usage
{
    /bin/cat <<-_BASIC_INFO_
    Usage: run.sh <action>
        <action>  Specify action name,
              'build-bundle | build-bundle-and-deploy | deploy-bundle'.
_BASIC_INFO_
}

# build the context bundle
function build_bundle
{
    echo "Building Context Bundle"
    nohup $JAVA $MODULE_JAVA_OPTS -Dmodule.name=$MODULE_NAME -jar ${SCRIPT_DIR}/lib/$APPLICATION_JAR $BUILD_JOB_PLAN_BUNDLE > ${SCRIPT_DIR}/logs/application.log 2>&1 &
}

# build the context bundle and deploy it
function build_and_deploy_bundle
{
    echo "Building and Deploying Context Bundle"
    nohup $JAVA $MODULE_JAVA_OPTS -Dmodule.name=$MODULE_NAME -jar ${SCRIPT_DIR}/lib/$APPLICATION_JAR $BUILD_AND_DEPLOY_JOB_PLAN_BUNDLE > ${SCRIPT_DIR}/logs/application.log 2>&1 &
}

# deploy the context bundle
function deploy_bundle
{
    echo "Deploying Context Bundle"
    nohup $JAVA $MODULE_JAVA_OPTS -Dmodule.name=$MODULE_NAME -jar ${SCRIPT_DIR}/lib/$APPLICATION_JAR $DEPLOY_JOB_PLAN_BUNDLE $FILE_TO_DEPLOY > ${SCRIPT_DIR}/logs/application.log 2>&1 &
}

ACTION=$1
case "$ACTION" in
    build-bundle)
        build_bundle
        ;;
    build-bundle-and-deploy)
        build_and_deploy_bundle
        ;;
    deploy-bundle)
        deploy_bundle
        ;;
    *)
        usage
        exit 1
        ;;
esac