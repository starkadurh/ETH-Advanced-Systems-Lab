#!/bin/bash


#file=
#if [[ $# > 0 ]]; then
    #file="$1"
#fi


#scp $file client1:.
#scp $file client2:.
#scp $file client3:.
#scp $file mw1:.
#scp $file mw2:.
#scp $file server1:.
#scp $file server2:.
#scp $file server3:.

cmd="rm authorized_keys"

ssh client1 "$cmd"
ssh client2 "$cmd"
ssh client3 "$cmd"
ssh mw1  "$cmd"
ssh mw2  "$cmd"
ssh server1 "$cmd"
ssh server2 "$cmd"
ssh server3 "$cmd"

#ssh client1 "cat $file >> .ssh/authorized_keys && rm $file"
#ssh client2 "cat $file >> .ssh/authorized_keys && rm $file"
#ssh client3 "cat $file >> .ssh/authorized_keys && rm $file"
#ssh mw1 "cat $file >> .ssh/authorized_keys && rm $file"
#ssh mw2 "cat $file >> .ssh/authorized_keys && rm $file"
#ssh server1 "cat $file >> .ssh/authorized_keys && rm $file"
#ssh server2 "cat $file >> .ssh/authorized_keys && rm $file"
#ssh server3 "cat $file >> .ssh/authorized_keys && rm $file"
echo "done"
