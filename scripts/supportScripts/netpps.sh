#!/bin/bash

timestamp() {
  date +"%Y-%m-%d_%H-%M-%S"
}

interval=1

if [[ $# > 0 ]]; then
	interval="$1"
fi

echo "Timestamp,tx eth0 (pkts/s),rx eth0 (pkts/s)"

while true
do
        R1=`cat /sys/class/net/eth0/statistics/rx_packets`
        T1=`cat /sys/class/net/eth0/statistics/tx_packets`
        sleep $interval
        R2=`cat /sys/class/net/eth0/statistics/rx_packets`
        T2=`cat /sys/class/net/eth0/statistics/tx_packets`
        
        TXPPS=$(awk "BEGIN {printf \"%.2f\", (${T2} - ${T1})/${interval}}")
        RXPPS=$(awk "BEGIN {printf \"%.2f\", (${R2} - ${R1})/${interval}}")
        echo "$(timestamp),${TXPPS},${RXPPS}"
done
