#!/bin/sh
RUN_PARAM_JAVA_OPTS=${TEMPLATE_RUN_PARAM_JAVA_OPTS}
RUN_PARAM_DEBUG_PORT=${TEMPLATE_RUN_PARAM_DEBUG_PORT}
RUN_PARAM_APP_NAME=${TEMPLATE_RUN_PARAM_APP_NAME}
SKYWALKING_ADDRESS=${TEMPLATE_SKYWALKING_ADDRESS}
SKYWALKING_NAMESPACE=${TEMPLATE_SKYWALKING_NAMESPACE}
TEMPLATE_JVM_MEMORY_LIMIT_MIN=${TEMPLATE_JVM_MEMORY_LIMIT_MIN}
JAR_NAME=`ls *.jar | head -n 1 | awk '{print \$0}'`
RUN_PARAM_DEBUG=""
SKYWALKING_PARAM=""
JVM_MEMORY_LIMIT=""
if [[ "\${RUN_PARAM_DEBUG_PORT}" != "" ]]; then
       RUN_PARAM_DEBUG="-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=\${RUN_PARAM_DEBUG_PORT}"
fi
if [[ "\${SKYWALKING_ADDRESS}" != "" ]]; then
       SKYWALKING_PARAM="-javaagent:/opt/path/skywalking/skywalking-agent/skywalking-agent.jar -Dskywalking.agent.namespace=\${SKYWALKING_NAMESPACE} -Dskywalking.agent.service_name=\${RUN_PARAM_APP_NAME} -Dskywalking.collector.backend_service=\${SKYWALKING_ADDRESS}"
fi
if [[ "\${TEMPLATE_JVM_MEMORY_LIMIT_MIN}" == 1 ]]; then
#        -XX:+UseG1GC -Xms128M -Xmx128M -XX:MaxGCPauseMillis=95
       JVM_MEMORY_LIMIT="-Xms128M -Xmx128M"
fi
nohup /usr/local/jdk/jdk8/bin/java \${SKYWALKING_PARAM} \${JVM_MEMORY_LIMIT} -Djava.security.egd=file:/dev/./urandom \${RUN_PARAM_DEBUG} -Dapp.name=\${RUN_PARAM_APP_NAME} -jar \${RUN_PARAM_JAVA_OPTS} \${JAR_NAME} >/dev/null 2>&1 &
echo \$! > tpid