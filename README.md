# Middleware

####  In my middleware there are the following classes:
##### RunMW
The provided file containing the main function that stars the middleware
##### MyMiddleware 
This is the net-thread. Before handling incoming connections and requests, it creates the worker threads, registers the shutdown hook and creates the request queue. There after it is ready to accept incoming connections and and handle incoming requests.
##### ReadHandler
Reads incoming requests from the SocketChannel, in case a partial request is read, it is stored internally in the ReadHandler while the network thread can continue to handle other requests. When the rest of the request is read and a complete request has been identified, ReadHanler calls the appropriate constructor depending on the request type.
##### GetRequest
Class that represents a get request.
##### SetRequest 
Class that represents a set request.
##### Request
Abstract class that both GetRequest and SetRequest subtype, the request queue uses this type.
##### Worker
The Worker class is the worker thread in the system, it takes requests from the queue and sends them to the server/s. If the request is a sharded multi-get requests it sends one shard of the request to each of the servers before waiting for all of the replies.
##### Coordinator
Simple class that implements the round-robin scheme to balance load between the servers. 
##### MWSocket
Wrapper for the socket that the worker thread uses to communicate with the server. When reading a response to a get request, it waits until the correct number of bytes has been read before it returns.
##### StatsHandler 
Collects all the statistics for a worker thread. Each worker thread has an instance of this class. It aggregates the statistics over a window of time and uses log4J to write it to file.
##### ShutdownHook
Handels the aggregation of the response time histogram data from all the worker threads and also calculates the cache miss ratio and collects any error messages any worker thread might haver encountered. As the statistics of individual threads are aggregated off-line, the ShutdownHook does not collect any other data.



Folder structure and naming conventions
------

#### Folder structure of experimental data
The folder structure and naming conventions of my results files is as follows:

```
data
  Results_mw_10.0.0.8		  - For results collected from the client Results_client_10.0.0.4
    2.1_2018-11-16_09-08-28	  - Experiment_Timestamp
      1:0		          - Work load, e.g. 0:1, 1:1, 1:6
        wt0_vc25_ct2		  - nr of worker threads _ virtual clients per thread _ threads per memtier instance
                                        For experiment 6 this directory's name is: 
                                        wt32_mw1_mc2 = nr of worker threads _ nr of middlewares _ nr of memcache servers
          rep1                    - repitition rep1, rep2, rep3
          
  pickleJar                       - Contains pickled Pandas DataFrames that contain the experimental data above
                                        the * represents the same Experiment_Timestamp as mentioned above
    2.1_*_client.pkl              - DF containing all data from the client.json filse for experiment 2.1
    2.1_*_client_full.pkl         - DF containing all data from the csv files from individual virtual clients 
    3.1_*_mw.pkl                  - DF containing all data from the stats.csv from each middleware
    5.1_*_client_hist.pkl         - DF containing all histogram data from the client.json files for the specified experiment
    5.1_*_mw_hist.pkl             - DF containing all histogram data from the results.log files for the specified experiment
```

These file contain data that was use for plotting, calculations and other analytics

#### Middleware
The files that are generated on the Middleware VM during a single run are as follows:
```
stats.csv               - Log entries for each individual thread during the experiment run
                            Statistics from StatsHandler
info.log                - General information logged by the middleware
mw.log                  - All output from the middleware
results.log             - Response time histogram, miss rate and error messages
                            Generated by ShutdownHook
ping.tar                - archived ping log files and network and system statistics
ping
  6 * ping-<ip>.log     - Output of ping to all client and server VMs
  kbs.csv               - Network usage in kb/s
  pps.csv               - Network usage in packets/s
  systemstats.csv       - RAM, CPU and disk usage for every seconds during run
```
#### Memtier
The files that are generated on the Memtier VM during a single run are as follows:
```
1 instance on each VM
  client.json           - The JSON output file that memtier generated
  client.log            - stdout of memtier
  ping.tar              - Same contents as for the middleware VM
  clientData.tar        - archived files generated by the --client-stats flag
  clientData 
    client-1-0-1.csv    - individual output files, one for each virtual client

2 instances on each VM
  client1.json          - If there are two instances running on each VM 
  client1.log           - Then the files are numbered like this.
  client2.json       
  client2.log
  ping.tar
  clientData.tar     	- archived files generated by the --client-stats flag
  clientData             
    client1-1-0-1.csv
    client2-1-0-1.csv
```
    		
#### Other folders and files
###### scripts
The directory scripts contains the scripts used to run the experiments.
experiments.sh is the main script and runs locally or on a dedicated vm in Azure.
run_memtier.sh runs on the memtier VM and is started in the experiments script with ssh
run_mw.sh runs on the middleware VM an is started in the experiments script with ssh
The directory supportScripts contains a variety of scripts that are either used by any of the afforementioned scrips or just used in isolation to simplify workflow.

###### processing
The directory processing contains two python files used to read all the experimental data and generate pandas dataframes.
parser.py parses all data for chapters 2-5 in the report and parser_6.py parses data for chapter 6.
Then there are Jupyter notebooks, one for each chapter 2-7.
They contain all the furhter processing and ploting of the data that was needed to write each chapter.
Within that directory is also the directory figures where the figures generated where saved.
All the dataframes used to in the plots and figures where dumped into csv files in the data folder with the same name as the figure.
Also contains network.m wich was used to calculate the network of queues.


			
