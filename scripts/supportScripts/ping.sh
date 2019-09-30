#!/bin/bash

dir=$(pwd)
interval=1

myIP=$(ifconfig | grep -Eo 'inet (addr:)?([0-9]*\.){3}[0-9]*' | grep -Eo '([0-9]*\.){3}[0-9]*' | grep -v '127.0.0.1')

clientIPs=("10.0.0.11" "10.0.0.4" "10.0.0.5")
mwIPs=("10.0.0.8" "10.0.0.7")
serverIPs=("10.0.0.9" "10.0.0.6" "10.0.0.10")

containsIP () {
  local e match="$1"
  shift
  for e; do [[ "$e" == "$match" ]] && return 1; done
  return 0
}

#Check if there is an argument to the script
if [[ $# > 0 ]]; then
	dir="$1"
fi

#Check if there is an argument to the script
if [[ $# > 1 ]]; then
interval="$2"
fi

mkdir -p $dir/ping

dir=$dir/ping

# Start other monitoring scripts
./$(dirname "$0")/netpps.sh $interval > $dir/pps.csv &
./$(dirname "$0")/netspeed.sh $interval > $dir/kbs.csv &
./$(dirname "$0")/systemStats.sh $interval > $dir/systemstats.csv &

# If run on a MW machine  
containsIP "$myIP" "${mwIPs[@]}"
if [ $? == 1 ]; then
    for ip in "${clientIPs[@]}"; do
        ping -D -i 0.2 ${ip} > $dir/ping-${ip}.log &
    done
    for ip in "${serverIPs[@]}"; do
        ping -D -i 0.2 ${ip} > $dir/ping-${ip}.log &
    done
fi

# If run on a client machine  
containsIP "$myIP" "${clientIPs[@]}"
if [ $? == 1 ]; then
    for ip in "${mwIPs[@]}"; do
        ping -D -i 0.2 ${ip} > $dir/ping-${ip}.log &
    done
    for ip in "${serverIPs[@]}"; do
        ping -D -i 0.2 ${ip} > $dir/ping-${ip}.log &
    done
fi

echo "pings are running."
