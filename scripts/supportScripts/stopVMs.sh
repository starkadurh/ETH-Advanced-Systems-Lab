#!/bin/bash


echo "Stopping all servers"

az vm deallocate --ids $(az vm list -d --query "[?powerState!='VM deallocated'].id" -o tsv) --no-wait

echo
echo "Waiting for shutdown"
sleep 20

running=$(az vm list -d --query "length([?powerState=='VM running'])")

timeout=0
while [ $running -gt 0 ]; do
    running=$(az vm list -d --query "length([?powerState=='VM running'])")
    let timeout=timeout+1
    if [[ $timeout -eq 30 ]]; then
        echo "Shutdown taking to long, something went wrong"
        exit
    fi
done

echo "Done: $running of 8 VMs running"

