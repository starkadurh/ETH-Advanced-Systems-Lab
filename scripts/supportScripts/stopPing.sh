#!/bin/bash

timestamp() {
  date +"%Y-%m-%d_%H-%M-%S"
}

# stop ping in background
kill -SIGINT `pgrep ping`
pkill netpps.sh
pkill netspeed.sh

echo "$(timestamp) - Ping stopped"
