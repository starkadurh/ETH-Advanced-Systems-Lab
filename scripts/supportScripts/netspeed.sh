#!/bin/bash

timestamp() {
  date +"%Y-%m-%d_%H-%M-%S"
}

interval=1

if [[ $# > 0 ]]; then
    interval="$1"
fi

echo "Timestamp,tx eth0 (kb/s),rx eth0 (kb/s)"

while true
do
        R1=`cat /sys/class/net/eth0/statistics/rx_bytes`
        T1=`cat /sys/class/net/eth0/statistics/tx_bytes`
        sleep $interval
        R2=`cat /sys/class/net/eth0/statistics/rx_bytes`
        T2=`cat /sys/class/net/eth0/statistics/tx_bytes`


        TBPS=$(awk "BEGIN {printf \"%.2f\", (${T2} - ${T1})/${interval}}")
        RBPS=$(awk "BEGIN {printf \"%.2f\", (${R2} - ${R1})/${interval}}")

        TKBPS=$(awk "BEGIN {printf \"%.2f\", ${TBPS}/ 1024 }")
        RKBPS=$(awk "BEGIN {printf \"%.2f\", ${RBPS}/ 1024 }")
        
        echo "$(timestamp),${TKBPS},${RKBPS}"
done
