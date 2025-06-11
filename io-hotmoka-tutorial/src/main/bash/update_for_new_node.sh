#!/bin/bash

# This script updates the "replacements.sh" script
# so that it reflects the content of a remote node.
# It is useful after a new node has been deployed, if we want the
# documentation and the tutorial examples
# to reflect the actual content of the node.

# Run for instance this way:
# NETWORK_URI="ws://mynode:myport" ./update_for_new_node.sh

# by default, it reflects the panarea.hotmoka.io:8001 node
NETWORK_URI=${NETWORK_URI:=ws://panarea.hotmoka.io:8001}

# by default, it modifies the shell script for Hotmoka
TYPE=hotmoka
TYPE_CAPITALIZED=${TYPE^}
SCRIPT=replacements_old.sh
DOCKER_HUB_USER=hotmoka

RED='\033[1;31m'
NC='\033[0m'
message() {
    printf "${RED}$@${NC}\n"
}

message "Updating file $SCRIPT by replaying its examples on the Hotmoka node at ${NETWORK_URI}"

echo "  Server = $NETWORK_URI"
echo "  Script = $SCRIPT"
echo "  Docker Hub's user = $DOCKER_HUB_USER"

VERSION=$(moka node info --json --uri $NETWORK_URI | python3 -c "import sys, json; print(json.load(sys.stdin)['version'])")
echo "  $TYPE_CAPITALIZED version = $VERSION"

message "Starting Docker container"
DOCKER_TOTAL_SUPPLY=1000000000000000
RUN=$(moka create-key --password-of-new-key=king --interactive=false)
LINE2=$(echo "$RUN"| sed '2!d')
NEW_DOCKER_KEY=${LINE2:19}
echo "  new docker key = $NEW_DOCKER_KEY"
sed -i "/@new_docker_key/s/\/.*\//\/@new_docker_key\/$NEW_DOCKER_KEY\//" $SCRIPT
CONTAINER_ID1=$(docker run --rm -dit -e INITIAL_SUPPLY=$DOCKER_TOTAL_SUPPLY -e KEY_OF_GAMETE=$NEW_DOCKER_KEY -e CHAIN_ID=caterpillar -e OPEN_UNSIGNED_FAUCET=true -e TIMEOUT_COMMIT=1 -p 8001:8001 -p 26656:26656 -v chain:/home/$TYPE/chain $DOCKER_HUB_USER/tendermint-node:$VERSION init)
echo "  container id = $CONTAINER_ID1"
sed -i "/@container_id1/s/\/.*\//\/@container_id1\/$CONTAINER_ID1\//" $SCRIPT

message "Waiting for the container to start"
sleep 20

RUN=$(moka info)
LINE=$(echo "$RUN" | grep takamakaCode)
DOCKER_TAKAMAKA_CODE=${LINE: -64}
echo "  docker's takamaka code = $DOCKER_TAKAMAKA_CODE"
sed -i "/@docker_takamaka_code/s/\/.*\//\/@docker_takamaka_code\/$DOCKER_TAKAMAKA_CODE\//" $SCRIPT

LINE=$(echo "$RUN" | grep manifest)
DOCKER_MANIFEST=${LINE: -66}
echo "  docker's manifest = $DOCKER_MANIFEST"
sed -i "/@docker_manifest/s/\/.*\//\/@docker_manifest\/$DOCKER_MANIFEST\//" $SCRIPT

LINE=$(echo "$RUN" | grep gamete)
DOCKER_GAMETE=${LINE: -66}
echo "  docker's gamete = $DOCKER_GAMETE"
sed -i "/@docker_gamete/s/\/.*\//\/@docker_gamete\/$DOCKER_GAMETE\//" $SCRIPT

LINE=$(echo "$RUN" | grep "validators" | sed '1!d')
DOCKER_VALIDATORS=${LINE: -66}
echo "  docker's validators = $DOCKER_VALIDATORS"
sed -i "/@docker_validators/s/\/.*\//\/@docker_validators\/$DOCKER_VALIDATORS\//" $SCRIPT

LINE=$(echo "$RUN" | grep "validator #0" | sed '1!d')
DOCKER_VALIDATOR0=${LINE: -66}
echo "  docker's validator #0 = $DOCKER_VALIDATOR0"
sed -i "/@docker_validator0/s/\/.*\//\/@docker_validator0\/$DOCKER_VALIDATOR0\//" $SCRIPT

LINE=$(echo "$RUN" | grep "id:" | sed '1!d')
DOCKER_ID_VALIDATOR0=${LINE: -40}
echo "  docker's validator #0 id = $DOCKER_ID_VALIDATOR0"
sed -i "/@docker_id_validator0/s/\/.*\//\/@docker_id_validator0\/$DOCKER_ID_VALIDATOR0\//" $SCRIPT

message "Binding the key of the gamete"
moka bind-key $NEW_DOCKER_KEY > /dev/null

message "Stopping the Docker container"
docker stop $CONTAINER_ID1 >/dev/null

message "Resuming the Docker container"
CONTAINER_ID2=$(docker run --rm -dit -p 8001:8001 -p 26656:26656 -v chain:/home/$TYPE/chain $DOCKER_HUB_USER/tendermint-node:$VERSION resume)
echo "  container id = $CONTAINER_ID2"
sed -i "/@container_id2/s/\/.*\//\/@container_id2\/$CONTAINER_ID2\//" $SCRIPT

message "Waiting for the container to start"
sleep 15

message "Stopping the Docker container"
docker stop $CONTAINER_ID2 >/dev/null

message "Resuming the Docker container"
CONTAINER_ID3=$(docker run --rm -dit -p 8001:8001 -p 26656:26656 -v chain:/home/$TYPE/chain $DOCKER_HUB_USER/tendermint-node:$VERSION resume)
echo "  container id = $CONTAINER_ID3"
sed -i "/@container_id3/s/\/.*\//\/@container_id3\/$CONTAINER_ID3\//" $SCRIPT

message "Waiting for the container to start"
sleep 15

message "Creating a new account"
RUN=$(moka create-account 1234567 --payer $DOCKER_GAMETE --password-of-payer=king --password-of-new-account=rock-and-roll --interactive=false)

LINE=$(echo "$RUN" | grep "A new account")
DOCKER_NEW_ACCOUNT=${LINE:14:66}
echo "  docker's new account = $DOCKER_NEW_ACCOUNT"
sed -i "/@docker_new_account/s/\/.*\//\/@docker_new_account\/$DOCKER_NEW_ACCOUNT\//" $SCRIPT

LINE=$(echo "$RUN" | grep "Total gas consumed")
DOCKER_TOTAL_GAS_NEW_ACCOUNT=${LINE:25}
echo "  docker's total gas for new account = $DOCKER_TOTAL_GAS_NEW_ACCOUNT"
sed -i "/@docker_total_gas_new_account/s/\/.*\//\/@docker_total_gas_new_account\/$DOCKER_TOTAL_GAS_NEW_ACCOUNT\//" $SCRIPT

RUN=$(moka info)
LINE=$(echo "$RUN" | grep "balance:" | sed '1!d')
DOCKER_REDUCED_BALANCE=${LINE:18}
echo "  docker's gamete balance after creation of account = $DOCKER_REDUCED_BALANCE"
sed -i "/@docker_reduced_balance/s/\/.*\//\/@docker_reduced_balance\/$DOCKER_REDUCED_BALANCE\//" $SCRIPT

LINE=$(echo "$RUN" | grep "balance:" | sed '2!d')
DOCKER_BALANCE_VALIDATOR0=${LINE:21}
echo "  docker's validator #0 balance = $DOCKER_BALANCE_VALIDATOR0"
sed -i "/@docker_balance_validator0/s/\/.*\//\/@docker_balance_validator0\/$DOCKER_BALANCE_VALIDATOR0\//" $SCRIPT

LINE=$(echo "$RUN" | grep "staked:" | sed '2!d')
DOCKER_STAKED_VALIDATOR0=${LINE:20}
echo "  docker's validator #0 staked balance = $DOCKER_STAKED_VALIDATOR0"
sed -i "/@docker_staked_validator0/s/\/.*\//\/@docker_staked_validator0\/$DOCKER_STAKED_VALIDATOR0\//" $SCRIPT
let DOCKER_DIFF1=$DOCKER_TOTAL_SUPPLY-$DOCKER_REDUCED_BALANCE
sed -i "/@docker_diff1/s/\/.*\//\/@docker_diff1\/$DOCKER_DIFF1\//" $SCRIPT
let DOCKER_DIFF2=$DOCKER_DIFF1-1234567
sed -i "/@docker_diff2/s/\/.*\//\/@docker_diff2\/$DOCKER_DIFF2\//" $SCRIPT
let DOCKER_SUM1=$DOCKER_BALANCE_VALIDATOR0+$DOCKER_STAKED_VALIDATOR0
sed -i "/@docker_sum1/s/\/.*\//\/@docker_sum1\/$DOCKER_SUM1\//" $SCRIPT
let DOCKER_DIFF3=$DOCKER_SUM1-$DOCKER_DIFF2
sed -i "/@docker_diff3/s/\/.*\//\/@docker_diff3\/$DOCKER_DIFF3\//" $SCRIPT

message "Stopping the Docker container"
docker stop $CONTAINER_ID3 >/dev/null

java --module-path modules/explicit_or_automatic --class-path modules/unnamed --add-modules org.glassfish.tyrus.container.grizzly.server,org.glassfish.tyrus.container.grizzly.client --module io.hotmoka.tutorial/io.hotmoka.tutorial.UpdateForNewNode $NETWORK_URI
