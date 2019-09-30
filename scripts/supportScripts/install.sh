#!/bin/bash
sudo apt-get --assume-yes update
sudo apt-get --assume-yes install memcached git unzip ant openjdk-8-jdk
wget https://github.com/RedisLabs/memtier_benchmark/archive/master.zip
unzip master.zip
cd memtier_benchmark-master
sudo apt-get --assume-yes install build-essential autoconf automake libpcre3-dev libevent-dev pkg-config zlib1g-dev
autoreconf -ivf
./configure
make
sudo service memcached stop
