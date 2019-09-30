#!/bin/bash

timestamp() {
  date +"%Y-%m-%d_%H-%M-%S"
}

interval=1

if [[ $# > 0 ]]; then
	interval="$1"
fi
echo "timestamp,Memory Usage,CPU Load,Disk Usage"
while true
do
    ram=$(free -m | awk 'NR==2{printf "%s/%sMB (%.2f%%)\n", $3,$2,$3*100/$2 }')
    cpu=$(top -bn 1 | grep load | awk '{printf "%.2f\n", $(NF-2)}')
    disk=$(df -h | awk '$NF=="/"{printf "%d/%dGB (%s)\n", $3,$2,$5}')
    echo "$(timestamp),${ram},${cpu},${disk}"
    sleep $interval
done

