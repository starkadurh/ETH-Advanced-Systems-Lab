#!/bin/bash

timestamp() {
  date +"%Y-%m-%d_%H-%M-%S"
}

######################################
#
# Run memcached servers 
#
######################################

startServers() {
    if [ $startServers == "true" ]; then
        # Run server
        if [ $serverMachines -gt 0 ]; then
            echo "  Starting Servers"
            i=1
            while [  $i -lt $(($serverMachines+1)) ]; do

                echo "    Starting Server number $i"

                # kill last tmux session if it is running
                ssh "server$i" "tmux ls > /dev/null 2>&1 && tmux kill-session -t server"

                # run memchached deamon
                ssh "server$i" 'tmux new -s server -d "memcached -t 1 -u root -l 0.0.0.0:11211"'


                let i=i+1
            done
        fi

        echo "  Finished starting mecahced servers"
    fi
}

#####################################
#
# Pepareration, pull git and build
#
#####################################

pullNbuild() {
    if [ $doPull == "true" ]; then
        echo "  Starting Preperation"
        if [ $clientMachines -gt 0 ]; then

            i=1
            while [  $i -lt $(($clientMachines+1)) ]; do
                echo "    Pulling Git repo on client$i"
                ssh "client$i" "git -C ${mwdir} pull"
                let i=i+1
            done
        fi


        if [ $mwMachines -gt 0 ]; then
            echo "  Building middleware"

            i=1
            while [  $i -lt $(($mwMachines+1)) ]; do

                echo "    Pulling Git repo and building on mw$i "
                ssh "mw$i" "git -C ${mwdir} pull"

                # kill last tmux session if it is running
                ssh "mw$i" "tmux ls > /dev/null 2>&1 && tmux kill-session -t mw"

                if [ $doCompile == "true" ]; then
                    ssh "mw$i" "ant -buildfile ${mwdir}/build.xml" 2>&1
                    echo ""
                fi

                let i=i+1
            done
        fi
    fi
}

######################################
#
# Run experiments 
#
######################################

######################
# helper functions 
######################

function contains() {
    local n=$#
    local value=${!n}
    for ((j=1;j < $#;j++)) {
        if [ "${!j}" == "${value}" ]; then
            echo "y"
            return 0
        fi
    }
    echo "n"
    return 1
}

recoverFromHalt() {
    i=1
    if [ "$1" = "mw" ]; then
        while [ $i -le 2 ]; do
            # kill last tmux session if it is running
            echo "      Try to recover. Killing $1 $i."
            ssh "$1$i" "tmux ls > /dev/null 2>&1 && tmux kill-session -t $1"
            let i=i+1
        done
    else
        while [ $i -le 3 ]; do
            # kill last tmux session if it is running
            echo "      Try to recover. Killing $1 $i."
            ssh "$1$i" "pkill memtier"
            #ssh "$1$i" "tmux ls > /dev/null 2>&1 && tmux kill-session -t $1"
            let i=i+1
        done
    fi
}

waitForMWReady() {
    timeout=0
    sleep 1
    i=1
    while [ $i -le $mwMachines ]; do
        while [ $(ssh "mw$i" "cat /tmp/middleware | grep running | wc -l") != 1 ]
        do
            #echo "      still waiting for mw$i ready"
            sleep 1
            # timeout if this takes to long
            let timeout=timeout+1
            if [ $timeout -gt 15 ]; then
                echo "      Waiting for mw$i took to long."
                recoverFromHalt "mw"
                return 0
            fi
        done 
        let i=i+1
    done
    #echo "      Middlewares are ready"
}

waitForClientFinish() {
    timeout=0
    sleep $1
    i=1
    while [ $i -le $clientMachines ]; do
        while [ $(ssh "client$i" "$(echo tmux ls) | grep client | wc -l" 2> /dev/null) == 1 ]
        do
            echo "      still waiting for client$i ($timeout)"
            sleep 2
            # timeout if this takes to long
            let timeout=timeout+1
            if [ $timeout -gt 15 ]; then
                echo "      Waiting for client$i took to long."
                recoverFromHalt "client"
                #recoverFromHalt "mw"       #should not be needed
                return 0
            fi
        done 
        let i=i+1
    done
    #echo "       Memtier runs have finished"
}

copyDataToLocalMachine() {
    echo "    Copying experiment results to local machine"
    i=1
    while [ $i -le $mwMachines ]; do 
        echo "      Copying from mw$i"
        scp -r "mw$i":Results_* $localResults
        scp -r "mw$i":stdout $localResults

        echo "      Moving files to archive"
        ssh "mw$i" "mkdir -p Archive && mv Results_*/* Archive && mv stdout Archive"

        let i=i+1
    done

    i=1
    while [ $i -le $clientMachines ]; do
        echo "      Copying from client$i"
        scp -r "client$i":Results_* $localResults
        scp -r "client$i":stdout $localResults

        echo "      Moving files to archive"
        ssh "client$i" "mkdir -p Archive && mv Results_*/* Archive && mv stdout Archive"

        let i=i+1
    done

}

serverAddresses=""
createServerAddressString(){

    serverAddresses=""
    let i=serverMachines-1
    while [ $i -ge 0 ]; do
        serverAddresses="${serverIPs[i]}:11211 $serverAddresses"
        let i=i-1
    done
    serverAddresses=$(printf "'"'%s'"'" "$serverAddresses")
}

printExperimentBegining() {
    echo "  ####################################################"
    echo "  $(timestamp) - Starting experiment ${experiment}"
    echo "  ####################################################"
}

printExperimentDone() {
    echo "  $(timestamp) - Experiment ${experiment} done."
    echo "  ####################################################"
    echo ""
}

populateServers(){
    i=1
    while [ $i -le $clientMachines ]; do
        echo "  Populating from client$i"
        ssh "client$i" "tmux new -s client -d \"~/$mwdir/scripts/supportScripts/populate.sh\""
        let i=i+1
    done

    i=1
    t=1
    while [ $i -le $clientMachines ]; do
        while [ $(ssh "client$i" "$(echo tmux ls) | grep client | wc -l" 2> /dev/null) == 1 ]
        do
            echo "      waiting for population to finish $t"
            sleep 1
            let t=t+1
        done 
        let i=i+1
    done
    echo "  Done populating"
}

runExperiment() {

    port=12345
    serverIp1=${mwIPs[0]}
    serverIp2=${mwIPs[1]}
    ct=1

    # settings for 2.1 & 2.2 without mw
    if [ $mwMachines -eq 0 ]; then
        port=11211
        serverIp1=${serverIPs[0]}
        serverIp2=${serverIPs[2]}

        # settings for 1 server if 0 mw servers, serverIp2 needs to be localhost 
        if [ $serverMachines -eq 1 ]; then 
            serverIp2="localhost"
            ct=2
        fi
        # setting for 1 middleware, serverIp2 needs to be localhost
    elif [ $mwMachines -eq 1 ]; then 
        serverIp2="localhost"
        ct=2
    fi

    printExperimentBegining
    experimentDir="${experiment}_$(timestamp)"
    experiment_names+=("${experimentDir}")

    createServerAddressString

    echo ""
    echo "    Memtier VMs:   ${clientMachines}      (inst/VM: $((2/ct))   thr/inst: $ct)"
    echo "    MW VMs:        ${mwMachines}"
    echo "    Memcache VMs:  ${serverMachines}"
    echo ""
    echo "    ServerAddress: ${serverAddresses}"
    echo "    ServerIp1:     ${serverIp1}"
    echo "    ServerIp2:     ${serverIp2}"
    echo "    Port:          ${port}"
    echo ""                 
    echo "    Workloads:     ${workloads[*]}"
    echo "    Clients:       ${clients[*]}"
    echo "    Worker Tr.:    ${workerThreads[*]}"
    echo "    Reps:          ${repetitions[*]}"
    echo ""                 
    echo "    Multigets:     ${multiget}"
    echo "    Sharded:       ${sharded}"
    echo "    Time:          ${time}"
    echo "    Directory:     ${experimentDir}"
    echo ""
    echo ""
    
    if [ $onlyPrintSettings != "true" ]; then
        for workload in "${workloads[@]}"; do
            for repetition in "${repetitions[@]}"; do
                for client in "${clients[@]}"; do
                    for workerThread in "${workerThreads[@]}"; do

                        if [ $mwMachines -gt 0 ]; then 
                            time_compensated=$(($time+($ct*$client*$clientMachines/((35*mwMachines))))) #compensation for high number of clients connecting to mw, longer warmup
                        else
                            time_compensated=$time
                        fi
                        echo "    $(timestamp) - workload=${workload} repetition=${repetition} client=${client} workerThread=${workerThread} time=${time_compensated}"

                        # directory for current repitition. Same dir structure for clients and mw
                        dir="${experimentDir}/${workload}/wt${workerThread}_vc${client}_ct${ct}/rep${repetition}"

                        # Start middleware
                        i=1
                        while [ $i -le $mwMachines ]; do
                            mw_out="stdout/${experimentDir}/mw${i}.out"
                            ssh "mw$i" "mkdir -p ~/stdout/${experimentDir}"
                            ssh "mw$i" "tmux new -s mw -d \"~/$mwdir/scripts/run_mw.sh $dir $workerThread $sharded $serverAddresses | tee -a ${mw_out}\""
                            let i=i+1
                        done

                        # wait untill Middleware is ready
                        waitForMWReady

                        # Run clients
                        multiget=${workload: -1}
                        multiget=$(($multiget>0?$multiget:1))

                        i=1
                        while [ $i -le $clientMachines ]; do
                            client_out="~/stdout/${experimentDir}/client${i}.out"
                            ssh "client$i" "mkdir -p ~/stdout/${experimentDir}"
                            ssh "client$i" "tmux new -s client -d \"~/$mwdir/scripts/run_client.sh $dir $client $workload $multiget $time_compensated ${port} ${serverIp1} ${serverIp2} | tee -a $client_out\""
                            let i=i+1
                        done

                        # wait untill client run has finished
                        waitForClientFinish $time_compensated

                        #stop middleware
                        i=1
                        while [ $i -le $mwMachines ]; do
                            ssh "mw$i" "tmux new -s killmw$i -d \"pkill java\""
                            let i=i+1
                        done
                    done
                done
            done
        done

        # copy data to local machine
        copyDataToLocalMachine
    fi

    printExperimentDone
}

########
# 2.1 
########

exp2_1() {
    if [ $(contains "${experiments[@]}" "2.1") == "y" ]; then

        experiment="2.1"

        clientMachines="3"
        mwMachines="0"
        serverMachines="1"

        workloads=("1:0" "0:1") # 1:0=WO  &  0:1=RO
        repetitions=(1 2 3)
        clients=(1 4 8 16 25 32 48 64 80 128 192)
        workerThreads=(0)

        sharded=false
        multiget=1
        time=60


        if [ $testRun == "true" ]; then
            experiment="${experiment}_test"
            workloads=("1:0" "0:1") # 1:0=WO  &  0:1=RO
            clients=(2)
            clients=(6 80 256)
            repetitions=(1)
            time=30
        fi

        runExperiment

    fi
}

########
# 2.2 
########

exp2_2() {
    if [ $(contains "${experiments[@]}" "2.2") == "y" ]; then

        experiment="2.2"

        clientMachines="1"
        mwMachines="0"
        serverMachines="2"

        repetitions=(1 2 3)
        clients=(1 4 8 16 25 32 48 64 80 128 192 256)
        workloads=("1:0" "0:1") # 1:0=WO  &  0:1=RO
        workerThreads=(0)

        sharded=false
        multiget=1
        time=60

        if [ $testRun == "true" ]; then
            experiment="${experiment}_test"
            workloads=("1:0" "0:1") # 1:0=WO  &  0:1=RO
            clients=(6 80 256)
            repetitions=(1)
            time=30
        fi

        runExperiment

    fi
}


########
# 3.1 
########

exp3_1() {
    if [ $(contains "${experiments[@]}" "3.1") == "y" ]; then

        experiment="3.1"

        clientMachines="3"
        mwMachines="1"
        serverMachines="1"

        repetitions=(1 2 3)
        clients=(1 6 12 18 25 32 48 64 80 128 196 256)
        workloads=("1:0" "0:1") # 1:0=WO  &  0:1=RO
        workerThreads=(8 16 32 64)

        sharded=false
        multiget=1
        time=60


        if [ $testRun == "true" ]; then
            experiment="${experiment}_test"
            workloads=("1:0" "0:1") # 1:0=WO  &  0:1=RO
            clients=(80 196)
            workerThreads=(8 64)
            repetitions=(1)
            time=30
        fi

        runExperiment

    fi
}

########
# 3.2 
########

exp3_2() {
    if [ $(contains "${experiments[@]}" "3.2") == "y" ]; then

        experiment="3.2"

        clientMachines="3"
        mwMachines="2"
        serverMachines="1"

        repetitions=(1 2 3)
        clients=(1 6 12 18 25 32 48 64 80 128 196 256)
        workloads=("1:0" "0:1") # 1:0=WO  &  0:1=RO
        workerThreads=(8 16 32 64)

        sharded=false
        multiget=1
        time=60

        if [ $testRun == "true" ]; then
            experiment="${experiment}_test"
            workloads=("1:0" "0:1") # 1:0=WO  &  0:1=RO
            clients=(6 80 196)
            workerThreads=(8 64)
            repetitions=(1)
            time=60
        fi

        runExperiment

    fi
}

########
# 4.1 
########

exp4_1() {
    if [ $(contains "${experiments[@]}" "4.1") == "y" ]; then

        experiment="4.1"

        clientMachines="3"
        mwMachines="2"
        serverMachines="3"

        repetitions=(1 2 3)
        clients=(1 6 12 18 25 32 48 64 80 128 196 256)
        workloads=("1:0") # 1:0=WO  &  0:1=RO
        workerThreads=(8 16 32 64)

        sharded=false
        multiget=1
        time=60


        if [ $testRun == "true" ]; then
            experiment="${experiment}_test"
            workloads=("1:0") # 1:0=WO  &  0:1=RO
            clients=(2 32)
            workerThreads=(8 64)
            repetitions=(1)
            time=10
        fi

        runExperiment

    fi
}

########
# 5.1 
########

exp5_1() {
    if [ $(contains "${experiments[@]}" "5.1") == "y" ]; then

        experiment="5.1"

        clientMachines="3"
        mwMachines="2"
        serverMachines="3"

        repetitions=(1 2 3)
        clients=(2)
        workloads=("1:1" "1:3" "1:6" "1:9") # 1:0=WO  &  0:1=RO  
        workerThreads=(8 16 32 64)

        sharded=true
        multiget=(1 3 6 9)
        time=60

        if [ $testRun == "true" ]; then
            experiment="${experiment}_test"
            workloads=("1:6") # 1:0=WO  &  0:1=RO
            clients=(2)
            workerThreads=(64)
            repetitions=(1 2)
            time=10
        fi

        runExperiment

    fi
}

########
# 5.2 
########

exp5_2(){
    if [ $(contains "${experiments[@]}" "5.2") == "y" ]; then

        experiment="5.2"

        clientMachines="3"
        mwMachines="2"
        serverMachines="3"

        repetitions=(1 2 3)
        clients=(2)
        workloads=("1:1" "1:3" "1:6" "1:9") # 1:0=WO  &  0:1=RO 
        workerThreads=(8 16 32 64)

        sharded=false
        multiget=(1 3 6 9)
        time=60

        if [ $testRun == "true" ]; then
            experiment="${experiment}_test"
            workloads=("1:6") # 1:0=WO  &  0:1=RO
            clients=(2)
            workerThreads=(32)
            repetitions=(1 2)
            time=10
        fi

        runExperiment

    fi
}

########
# 6 
########

exp6() {
    if [ $(contains "${experiments[@]}" "6") == "y" ]; then

        experiment="6"

        clientMachines="3"
        mwMachines="2"
        serverMachines="3"


        sharded=false
        multiget=1
        time=60


        clients=(32)
        workloads=("1:0" "0:1" "1:1") # 1:0=WO  &  0:1=RO  
        repetitions=(1 2 3)
        if [ $testRun == "true" ]; then
            experiment="${experiment}_test"
            workloads=("1:0" "0:1") # 1:0=WO  &  0:1=RO
            clients=(3)
            repetitions=(1)
            time=10

            printExperimentBegining
            experimentDir="${experiment}_$(timestamp)"
            experiment_names+=("${experimentDir}")


            mwMachines="2"
            serverMachines="3"
            workerThreads=(32) 
            runExperiment6

            mwMachines="1"
            serverMachines="1"
            workerThreads=(8) 
            runExperiment6

        else

            printExperimentBegining
            experimentDir="${experiment}_$(timestamp)"
            experiment_names+=("${experimentDir}")


            mwMachines="2"
            serverMachines="3"
            workerThreads=(8 32) 
            runExperiment6

            mwMachines="1"
            serverMachines="3"
            workerThreads=(8 32) 
            runExperiment6

            mwMachines="2"
            serverMachines="1"
            workerThreads=(8 32) 
            runExperiment6

            mwMachines="1"
            serverMachines="1"
            workerThreads=(8 32) 
            runExperiment6
        fi



        if [ $onlyPrintSettings != "true" ]; then
            # copy data to local machine
            clientMachines="3"
            mwMachines="2"
            serverMachines="3"
            copyDataToLocalMachine
        fi
        printExperimentDone
    fi
}

runExperiment6() {


    port=12345
    serverIp1=${mwIPs[0]}
    serverIp2=${mwIPs[1]}
    ct=1

    if [ $mwMachines -eq 1 ]; then 
        serverIp2="localhost"
        ct=2
    fi


    createServerAddressString

    echo ""
    echo "    Memtier VMs:   ${clientMachines}      (inst/VM: $((2/ct))   thr/inst: $ct)"
    echo "    MW VMs:        ${mwMachines}"
    echo "    Memcache VMs:  ${serverMachines}"
    echo ""
    echo "    ServerAddress: ${serverAddresses}"
    echo "    ServerIp1:     ${serverIp1}"
    echo "    ServerIp2:     ${serverIp2}"
    echo "    Port:          ${port}"
    echo ""
    echo "    Workloads:     ${workloads[*]}"
    echo "    Clients:       ${clients[*]}"
    echo "    Worker Tr.:    ${workerThreads[*]}"
    echo "    Reps:          ${repetitions[*]}"
    echo ""
    echo "    Multigets:     ${multiget}"
    echo "    Sharded:       ${sharded}"
    echo "    Time:          ${time}"
    echo "    Directory:     ${experimentDir}"
    echo ""
    echo ""
        
    client=32
    if [ $onlyPrintSettings != "true" ]; then
        for workload in "${workloads[@]}"; do
            for repetition in "${repetitions[@]}"; do
                for workerThread in "${workerThreads[@]}"; do

                    time_compensated=$(($time+($ct*$client*$clientMachines/((35*mwMachines))))) #compensation for high number of clients connecting to mw, longer warmup
                    echo "    $(timestamp) - workload=${workload} repetition=${repetition} client=${client} workerThread=${workerThread} time=${time_compensated}"

                    # directory for current repitition. Same dir structure for clients and mw
                    dir="${experimentDir}/${workload}/wt${workerThread}_mw${mwMachines}_mc${serverMachines}/rep${repetition}"

                    # Start middleware
                    i=1
                    while [ $i -le $mwMachines ]; do
                        mw_out="stdout/${experimentDir}/mw${i}.out"
                        ssh "mw$i" "mkdir -p ~/stdout/${experimentDir}"
                        ssh "mw$i" "tmux new -s mw -d \"~/$mwdir/scripts/run_mw.sh $dir $workerThread $sharded $serverAddresses | tee -a ${mw_out}\""
                        let i=i+1
                    done

                    # wait untill Middleware is ready
                    waitForMWReady

                    # Run client
                    multiget=${workload: -1}
                    multiget=$(($multiget>0?$multiget:1))


                    i=1
                    while [ $i -le $clientMachines ]; do
                        client_out="~/stdout/${experimentDir}/client${i}.out"
                        ssh "client$i" "mkdir -p ~/stdout/${experimentDir}"
                        ssh "client$i" "tmux new -s client -d \"~/$mwdir/scripts/run_client.sh $dir $client $workload $multiget $time_compensated ${port} ${serverIp1} ${serverIp2} | tee -a $client_out\""
                        let i=i+1
                    done

                    # wait untill client run has finished
                    waitForClientFinish $time_compensated

                    #stop middleware
                    i=1
                    while [ $i -le $mwMachines ]; do
                        ssh "mw$i" "tmux new -s killmw$i -d \"pkill java\""
                        let i=i+1
                    done
                done
            done
        done
    fi

}

########
# test 
########

exp_test() {
    if [ $(contains "${experiments[@]}" "test") == "y" ]; then

        experiment="test_All_VMs_multiget"

        clientMachines="3"
        mwMachines="2"
        serverMachines="3"

        repetitions=(1)
        clients=(15)
        workloads=("1:9") # 1:0=WO  &  0:1=RO
        workerThreads=(16)
        sharded=true
        multiget=9
        time=30

       runExperiment
    fi
}


shutdown(){

    if [ $onlyPrintSettings != "true" ]; then
        ./$(dirname "$0")/supportScripts/shutdown.sh "$1"
    fi
}

shutdownAll(){
    if [ $onlyPrintSettings != "true" ]; then
        ./$(dirname "$0")/supportScripts/stopVMs.sh
    fi
}

mwdir="asl-fall18-project"

myIp=$(ifconfig | grep -Eo 'inet (addr:)?([0-9]*\.){3}[0-9]*' | grep -Eo '([0-9]*\.){3}[0-9]*' | grep -v '127.0.0.1')

isLocal=false
if [ $myIp == "10.0.0.12" ]; then
    localResults="/home/starkadur/ASLResults"
else 
    localResults="/Users/SH/ASLResults"
    isLocal=true
fi

clientMachines="3"
mwMachines="2"
serverMachines="3"

clientIPs=("10.0.0.11" "10.0.0.4" "10.0.0.5")
mwIPs=("10.0.0.8" "10.0.0.7")
serverIPs=("10.0.0.9" "10.0.0.6" "10.0.0.10")

experiments=("3.1")
#experiments=("2.1" "2.2" "3.1" "3.2" "4.1" "5.1" "5.2")
#experiments=("2.1" "2.2" "3.1" "3.2" "4.1")
#experiments=("2.1" "3.1" "4.1")
#experiments=("3.1" "3.2")
#experiments=("6")
#experiments=("2.1" "2.2" "3.1" "3.2" "4.1" "5.1" "5.2" "6")


startServers="true"
doPull="true"
doCompile="true"

#startServers="false"
#doPull="false"
#doCompile="false"

testRun="true"
onlyPrintSettings="false"

if [ $onlyPrintSettings == "true" ]; then
    startServers="false"
    testRun="false"
    doPull="false"
    doCompile="false"
fi

experiment_names=()

startServers
pullNbuild

populateServers

exp6

exp5_1
exp5_2
exp4_1
exp3_2
#shutdown "mw2"
exp3_1
#shutdown "mw1"
exp2_1
#shutdown "client2 client3"
exp2_2
#shutdownAll


echo "Experiments run:"
for experiment_name in "${experiment_names[@]}"; do
    echo "    ${experiment_name}"
done
echo


echo "$(timestamp) - done."

