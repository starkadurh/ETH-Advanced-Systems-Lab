#!/bin/bash


timestamp() {
  date +"%Y-%m-%d_%H-%M-%S"
}

myIp=$(ifconfig | grep -Eo 'inet (addr:)?([0-9]*\.){3}[0-9]*' | grep -Eo '([0-9]*\.){3}[0-9]*' | grep -v '127.0.0.1')


#cmdpart="./memtier_benchmark-master/memtier_benchmark --data-size=4096 --protocol=memcache_text --expiry-range=999999-1000000 --key-maximum=10000 --show-config "
cmdpart="./memtier_benchmark-master/memtier_benchmark --data-size=32 --protocol=memcache_text --expiry-range=999999-1000000 --key-maximum=10000 --show-config "

directoryName=client_$(timestamp)
clients=1
workload="1:0"
multiget=="1"
time=80
port=12345
server="localhost"
server2="localhost"

#Check if there is directoryName argument to the script
if [[ $# > 0 ]]; then
	directoryName="$1"
fi

#Check if there is a server argument to the script
if [[ $# > 1 ]]; then
	clients="$2"
fi

#Check if there is a server argument to the script
if [[ $# > 2 ]]; then
	workload="$3"
fi

#Check if there is a server argument to the script
if [[ $# > 3 ]]; then
	multiget="$4"
fi

#Check if there is a time argument to the script
if [[ $# > 4 ]]; then
	time="$5"
fi

#Check if there is a server argument to the script
if [[ $# > 5 ]]; then
	port="$6"
fi

#Check if there is a server argument to the script
if [[ $# > 6 ]]; then
	server="$7"
fi

#Check if there is a server argument to the script
if [[ $# > 7 ]]; then
	server2="$8"
fi

testDir=Results_client_${myIp}/${directoryName}
mkdir -p "$testDir/clientData"

./asl-fall18-project/scripts/supportScripts/ping.sh $testDir 1 &

#add parameters to the command
if [ $server2 == "localhost" ]; then
    outputFile=client.log

    cmd="${cmdpart} --server=${server} --port=${port} --ratio=${workload} --multi-key-get=${multiget} --test-time=${time} --clients=${clients} --threads=2 --json-out-file=$testDir/client.json --client-stats=$testDir/clientData/client" 

    $cmd 2>&1 | tee $testDir/$outputFile
    
else
    outputFile1=client1.log
    outputFile2=client2.log

    cmd1="${cmdpart} --server=${server} --port=${port} --ratio=${workload} --multi-key-get=${multiget} --test-time=${time} --clients=${clients} --threads=1 --json-out-file=$testDir/client1.json --client-stats=$testDir/clientData/client1"
    cmd2="${cmdpart} --server=${server2} --port=${port} --ratio=${workload} --multi-key-get=${multiget} --test-time=${time} --clients=${clients} --threads=1 --json-out-file=$testDir/client2.json --client-stats=$testDir/clientData/client2"

    pids=""
    #run the command
    $cmd1 2>&1 | tee $testDir/$outputFile1 &
    pids="$pids $!"
    $cmd2 2>&1 | tee $testDir/$outputFile2 &
    pids="$pids $!"

    for f in $pids
    do
        wait $f
    done
fi

kill -SIGINT `pgrep ping`
pkill netpps.sh
pkill netspeed.sh
pkill systemStats.sh

# put all ping files and client data in tar archive and delete ping directory
tar cfz $testDir/ping.tar -C $testDir/ping .
rm -rf $testDir/ping

tar cfz $testDir/clientData.tar -C $testDir/clientData .
rm -rf $testDir/clientData


