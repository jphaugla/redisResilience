CLUSTER_NAME=cluster-1.redis-enterprise.com
CLUSTER_NAME2=cluster-2.redis-enterprise.com
ADMIN_U=admin@redis.com
ADMIN_P=redis123

#!/bin/bash
rm create_crdb.sh
tee -a create_crdb.sh << EOF
/opt/redislabs/bin/crdb-cli crdb create --name sample-crdb --password ${ADMIN_P} --memory-size 50mb --default-db-config '{ "port": 12000, "replication": false}' --instance fqdn=${CLUSTER_NAME},username=${ADMIN_U},password=${ADMIN_P} --instance fqdn=${CLUSTER_NAME2},username=${ADMIN_U},password=${ADMIN_P}
EOF
chmod 755 create_crdb.sh
docker cp create_crdb.sh re1:/opt/create_crdb.sh
docker exec -it re1 bash -c "/opt/create_crdb.sh"
docker exec -it re1 bash -c "rladmin status databases"
