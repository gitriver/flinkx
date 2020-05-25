#!/bin/sh
#-------------------------------------------------------------------------------------------------------------
#该脚本的使用方式为-->[sh run.sh]
#该脚本可在服务器上的任意目录下执行,不会影响到日志的输出位置等
#-------------------------------------------------------------------------------------------------------------
SCRIRPT="$0"

# Find the java binary
if [ -n "${JAVA_HOME}" ]; then
  RUNNER="${JAVA_HOME}/bin/java"
elif [ `command -v java` ]; then
  RUNNER="java"
else
  echo "JAVA_HOME is not set" >&2
  exit 1
fi


APP_HOME=`cd "$SCRIPT"; pwd`
APP_LOG=${APP_HOME}/logs
CLASSPATH=$APP_HOME/conf
for jarFile in ${APP_HOME}/lib/*.jar;
do
   CLASSPATH=$CLASSPATH:$jarFile
done

#参数处理
APP_MAIN="com.dtstack.flinkx.Main"

params=$@
JAVA_OPTS="-Duser.timezone=GMT+8 -server -Xms2048m -Xmx2048m -Xloggc:${APP_LOG}/gc.log -DLOG_DIR=${APP_LOG}"


startup(){
  aparams=($params)
  #echo "params len "${#aparams[@]}
  len=${#aparams[@]}
  for ((i=0;i<$len;i++));do
    echo "第${i}参数:${aparams[$i]}";
    str=$str" "${aparams[$i]};
  done
  echo "Starting $APP_MAIN"
  echo "$RUNNER $JAVA_OPTS -classpath $CLASSPATH $APP_MAIN $str"
  $RUNNER $JAVA_OPTS -classpath $CLASSPATH $APP_MAIN $str
}
startup
