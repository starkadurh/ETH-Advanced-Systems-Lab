#!/bin/bash


timestamp() {
  date +"%Y-%m-%d_%H-%M-%S"
}

myIp=$(ifconfig | grep -Eo 'inet (addr:)?([0-9]*\.){3}[0-9]*' | grep -Eo '([0-9]*\.){3}[0-9]*' | grep -v '127.0.0.1')

directoryName=client_$(timestamp)
threads=1
sharded="false"
server="localhost"

#Check if there is directoryName argument to the script
if [[ $# > 0 ]]; then
	directoryName="$1"
fi

#Check if there is a thread count argument to the script
if [[ $# > 1 ]]; then
	threads="$2"
fi

#Check if there is a sharded argument to the script
if [[ $# > 2 ]]; then
	sharded="$3"
fi

#Check if there is a server argument to the script
if [[ $# > 3 ]]; then
	server="$4"
fi


testDir=Results_mw_${myIp}/${directoryName}
mkdir -p $testDir

#add parameters to the command
outputFile=mw.log

mwdir="asl-fall18-project"
cmd="java -Dlog4j.configurationFile=${mwdir}/log4j2.properties -cp \"${mwdir}/dist/middleware-shrobjar.jar:${mwdir}/lib/*\" ch.ethz.asltest.RunMW -l 0.0.0.0 -p 12345 -t ${threads} -s ${sharded} -m ${server}"

# clears "running" from middleware file that is used for coordination
echo > /tmp/middleware

./asl-fall18-project/scripts/supportScripts/ping.sh $testDir 1 &

eval $cmd | tee $testDir/$outputFile 
wait $!

kill -SIGINT `pgrep ping`
pkill netpps.sh
pkill netspeed.sh
pkill systemStats.sh

cp logs/*.* $testDir/.

# put all ping files in tar archive and delete ping directory
tar cfz $testDir/ping.tar -C $testDir/ping .
rm -rf $testDir/ping



