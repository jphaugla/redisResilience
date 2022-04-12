#!/usr/bin/env bash


ADMIN_U=admin@redis.com
ADMIN_P=redis123

GREEN='\e[32;1m'
NC='\e[0m'

OK=${GREEN}ok${NC}

printf '\nBootstrap endpoint: '

until $(docker exec -it re1 sh -c "curl --output /dev/null --silent --head --fail -k https://localhost:9443/v1/bootstrap"); do
    printf '.'
    sleep 3
done
printf ' ready!\n'

printf '\nnode 1: '
docker exec -it re1 rladmin cluster create name cluster-1.redis-enterprise.com username $ADMIN_U password $ADMIN_P

printf '\nnode 2: '
docker exec -it re2 rladmin cluster join nodes 172.22.0.11 username $ADMIN_U password $ADMIN_P

printf '\nCluster 1 setup complete!\n'

until $(docker exec -it re3 sh -c "curl --output /dev/null --silent --head --fail -k https://localhost:9443/v1/bootstrap"); do
    printf '.'
    sleep 3
done
printf ' ready!\n'

printf '\nnode 3: '
docker exec -it re3 rladmin cluster create name cluster-2.redis-enterprise.com username $ADMIN_U password $ADMIN_P

printf '\nnode 4: '
docker exec -it re4 rladmin cluster join nodes 172.22.0.13 username $ADMIN_U password $ADMIN_P

printf '\nCluster 2 setup complete!\n'


printf "\n\nCreating IP routes - wait for updates..."
docker exec --user root re1 bash -c "iptables -t nat -I PREROUTING -p udp --dport 53 -j REDIRECT --to-ports 5300  >/dev/null"
docker exec --user root re2 bash -c "iptables -t nat -I PREROUTING -p udp --dport 53 -j REDIRECT --to-ports 5300  >/dev/null"
docker exec --user root re3 bash -c "iptables -t nat -I PREROUTING -p udp --dport 53 -j REDIRECT --to-ports 5300  >/dev/null"
docker exec --user root re4 bash -c "iptables -t nat -I PREROUTING -p udp --dport 53 -j REDIRECT --to-ports 5300  >/dev/null"
sleep 60
echo ok

#printf "\n\nTCPDUMP install on each node.\n\n"
#docker exec --user root re1 bash -c "apt-get install -y tcpdump"
#docker exec --user root re2 bash -c "apt-get install -y tcpdump"
#docker exec --user root re3 bash -c "apt-get install -y tcpdump"
#docker exec --user root re4 bash -c "apt-get install -y tcpdump"
#echo ok

sleep 1
echo -e "Finished!"
sleep 2
