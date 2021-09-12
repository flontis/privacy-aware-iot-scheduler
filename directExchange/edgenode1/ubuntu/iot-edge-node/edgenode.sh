docker build -t iftm-edge-node . && docker run -P --name edge-node -v /var/run/docker.sock:/var/run/docker.sock --network host iftm-edge-node 
