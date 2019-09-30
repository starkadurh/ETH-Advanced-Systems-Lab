#!/bin/bash

servers=("10.0.0.9" "10.0.0.6" "10.0.0.10")

for server in "${servers[@]}"; do
	./memtier_benchmark-master/memtier_benchmark -s "$server" -p 11211 -P memcache_text -c 1 -t 1 -n 10001 --key-pattern=S:S --ratio=1:0 --key-maximum=10000 --expiry-range=999999-1000000 --data-size=1024 --hide-histogram
done
