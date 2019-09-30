#!/bin/bash

client[0]='--resource-group ASL --name Client1'
client[1]='--resource-group ASL --name Client2'
client[2]='--resource-group ASL --name Client3'
mw[0]='--resource-group ASL --name Middleware1'
mw[1]='--resource-group ASL --name Middleware2'
server[0]='--resource-group ASL --name Server1'
server[1]='--resource-group ASL --name Server2'
server[2]='--resource-group ASL --name Server3'

#Check if there is an argument to the script
if [[ $# > 0 ]]; then
	clientCount=$1
else
	clientCount=1
fi

#Check if there is a 2nd argument to the script
if [[ $# > 1 ]]; then
	mwCount=$2
else
	mwCount=1
fi

#Check if there is a 2nd argument to the script
if [[ $# > 2 ]]; then
	serverCount=$3
else
	serverCount=1
fi

echo "Starting:"
echo "Clients = $clientCount"
echo "MWs = $mwCount"
echo "Servers = $serverCount"

i=0
while [ $i -lt "$clientCount" ]; do
    az vm start ${client[$i]} --no-wait
    echo "Starting client $((i+1))"
    let i=i+1 
done

i=0
while [ $i -lt "$mwCount" ]; do
    az vm start ${mw[$i]} --no-wait
    echo "Starting Middleware $((i+1))"
    let i=i+1 
done

i=0
while [ $i -lt "$serverCount" ]; do
    az vm start ${server[$i]} --no-wait
    echo "Starting server $((i+1))"
    let i=i+1 
done

echo
echo "Waiting for startup"
sleep 50

total=$((clientCount+mwCount+serverCount))
running=$(az vm list -d --query "length([?powerState=='VM running'])")

timeout=0
while [ $running -lt "$total" ]; do
    running=$(az vm list -d --query "length([?powerState=='VM running'])")
    let timeout=timeout+1
    if [[ $timeout -eq 30 ]]; then
        echo "Startup is taking to long, something went wrong, stoping all VMs"
        ./stopVMs.sh
        exit 1
    fi
done

echo "Done: $running of 8 VMs running"

exit 0

