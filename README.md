# Privacy aware IoT scheduler
This is the privacy-aware IoT scheduling system I examined in my master's thesis. I wrote all code on my own, EXCEPT the code inside of directories named "iftm".
These folders contain adjusted code written by Dominik Nafz in his master's thesis, so all rights and honours for this code go to him. For further reading, see [IFTM Project | https://github.com/dos-group/IFTM-Anomaly-Detection/tree/ADUManagement/simple-webservice]

# Assumptions
It is assumed that a network of five machines is available to run all scheduling modes. The VMs have to be connected differently for the different scheduling modes.

## Direct Scheduling
Edge Node 1 --- Edge Server
      \
        Edge Node 2

## Intra Community Scheduling
Edge Node 1 --- Edge Server --- Edge Node 2

## Inter Community Scheduling
Edge Node 1 --- Edge Server 1 --- Inter Community Server --- Edge Server 2 --- Edge Node 2

It is assumed, that the Edge Node 1 and Edge Server 1 (and ES2 and EN2, respectively) are in their own network/subnet, representing their community.
Also, each node should only be able to reach nodes it is connected to, so that the network topology can be drawn as a tree.

# Preconditions

Install the following dependencies on every machine:
* Java 11
* Docker
* Maven

Before running the program, distribute the content of the corresponding directories to the machines and make sure that each node is reachable 
and connected in the right way and that the IP addresses are correctly set in the main classes.

# Running the program

Start the programs in the following order:
1. Anomaly Detection Pipeline on the Edge Node 1 (via 'docker-compose -f simple-webservice-1-1-2-1-setup-compose.yml up -d' )
2. Start the ics ('sudo ./ics.sh')
3. Start the Edge Servers ('sudo ./edgeserver.sh')
4. Start the Edge Nodes ('sudo ./edge-node.sh')
