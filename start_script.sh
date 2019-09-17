#!/bin/ksh

########################################################################
# 0. Check if JAVA_HOME is set 

if [ x"$JAVA_HOME" = x ] 
then
	echo "JAVA_HOME must be set...!!!"
	exit 1
fi
if [ ! -x $JAVA_HOME/bin/java ]
then
	echo "$JAVA_HOME/bin/java must exist and be executable...!!!"
	exit 1
fi
START_JAVA=$JAVA_HOME/bin/java


########################################################################
# 1a. First check whether PRM_ROOT is set
if [ x"$PRM_ROOT" = x ]
then
	echo "PRM_ROOT must be set...!!!"
	exit 1
fi
if [ ! -d $PRM_ROOT ]
then
	echo "$PRM_ROOT directory must exist...!!!"
	exit 1
fi

########################################################################
# 2. Set PRM_LIB to default if NOT explicitly set

if [ x"$PRM_LIB" = x ]
then
	echo "PRM_LIB not set... PRM_LIB set to $PRM_ROOT/lib...!!!"
	export PRM_LIB=$PRM_ROOT/lib
fi

if [ ! -d $PRM_LIB ]
then
	echo "$PRM_LIB directory must exist...!!!"
	exit 1
fi

PRM_LIB_RUNTIME=$PRM_LIB/runtime
PRM_LIB_INTERNAL=$PRM_LIB/internal
PRM_LIB_THIRDPARTY=$PRM_LIB/thirdparty

if [ ! -d $PRM_LIB_RUNTIME ]
then
       echo " $PRM_LIB_RUNTIME missing..."
       exit 1
fi

if [ ! -d $PRM_LIB_INTERNAL ]
then
       echo " $PRM_LIB_INTERNAL missing..."
       exit 1
fi

if [ ! -d $PRM_LIB_THIRDPARTY ]
then
       echo " $PRM_LIB_THIRDPARTY missing..."
       exit 1
fi


# Prepare Class Path  ##################################################



CLASSPATH=`$PRM_ROOT/bin/buildclasspath.sh $PRM_ROOT/lib`
export CLASSPATH
echo "Starting with classpath...\n $CLASSPATH"


nohup $START_JAVA -classpath $CLASSPATH:IDET.jar  -Xms256m -Xmx2048m com.i2i.idet.base.IDET 

PID=$!

trap "echo Killing child pid $PID; kill -TERM $PID" 1 2 3 4 5 6 7 8 9 10 12 13 14 15 19 20

wait


