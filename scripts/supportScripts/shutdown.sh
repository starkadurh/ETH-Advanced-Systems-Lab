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
	vms="$1"
else
	exit 0
fi

for i in $vms; do
    if [ $i == "all" ]; then
        ./$(dirname "$0")/stopVMs.sh
        exit 0
    else
        machineType=$(echo $i | grep -o -E '[a-z]+' | head -1 | sed -e 's/^0\+//')
        id=$(echo $i | grep -o -E '[0-9]' | head -1 | sed -e 's/^0\+//')

        echo "Deallocating $machineType$id"
        let id=id-1

        machine=$machineType[$id]
        az vm deallocate ${!machine} --no-wait
    fi
done

