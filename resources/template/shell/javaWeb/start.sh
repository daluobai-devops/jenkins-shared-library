#!/bin/sh
RUN_PARAM_JAVA_OPTS=${TEMPLATE_RUN_PARAM_JAVA_OPTS}
JAVA_HOME=${TEMPLATE_JAVA_HOME}
RUN_PARAM_DEBUG_PORT=${TEMPLATE_RUN_PARAM_DEBUG_PORT}
RUN_PARAM_APP_NAME=${TEMPLATE_RUN_PARAM_APP_NAME}
JAR_NAME=`ls *.jar | head -n 1 | awk '{print \$0}'`
RUN_PARAM_DEBUG=""
if [[ "\${RUN_PARAM_DEBUG_PORT}" != "" ]]; then
       RUN_PARAM_DEBUG="-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=\${RUN_PARAM_DEBUG_PORT}"
fi
nohup \${JAVA_HOME}/bin/java -Xms128M -Xmx128M -Djava.security.egd=file:/dev/./urandom \${RUN_PARAM_DEBUG} -Dapp.name=\${RUN_PARAM_APP_NAME} -jar \${RUN_PARAM_JAVA_OPTS} \${JAR_NAME} >/dev/null 2>&1 &
echo \$! > tpid