#!/bin/bash

# This script updates the "replacements.sh" script
# so that it reflects the content of a remote node.
# It is useful after a new node has been deployed, if we want the
# documentation and the tutorial examples
# to reflect the actual content of the node.

# Run for instance this way:
# NETWORK_URI="ws://mynode:myport" ./update_for_new_node.sh

# by default, it reflects the panarea.hotmoka.io node
NETWORK_URI=${NETWORK_URI:=ws://panarea.hotmoka.io:8001}
NETWORK_URI_WITHOUT_PROTOCOL=$(echo $NETWORK_URI | sed s/".*:\/\/"/""/g) # remove protocol, if any

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

sed -i "/@server/s/\/.*\//\/@server\/ws:\\\\\/\\\\\/$NETWORK_URI_WITHOUT_PROTOCOL\//" $SCRIPT
VERSION=$(moka node info --json --uri $NETWORK_URI | python3 -c "import sys, json; print(json.load(sys.stdin)['version'])")
echo "  $TYPE_CAPITALIZED version = $VERSION"
sed -i '/@hotmoka_version/s/\/.*\//\/@hotmoka_version\/'$VERSION'\//' $SCRIPT

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

# HERE HERE HERE HERE

java --module-path modules/explicit_or_automatic --class-path modules/unnamed --add-modules org.glassfish.tyrus.container.grizzly.server,org.glassfish.tyrus.container.grizzly.client --module io.hotmoka.tutorial/io.hotmoka.tutorial.UpdateForNewNode $NETWORK_URI

message "Editing the \"Family.java\" run example from the tutorial"
sed -i '/ADDRESS = /s/".*"/"'$ACCOUNT1'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Family.java
sed -i '/URI.create(/s/".*"/"ws:\/\/'$NETWORK_URI_WITHOUT_PROTOCOL'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Family.java

message "Packaging the \"runs\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/runs/pom.xml package 2>/dev/null

message "Running the \"Family.java\" run example from the tutorial"
# we provide the private key of account1 so that the run works
cp $ACCOUNT1.pem ../../hotmoka_tutorial/
cd ../../hotmoka_tutorial/runs
RUN=$(java --module-path ../../hotmoka/io-hotmoka-moka/modules/explicit/:../../hotmoka/io-hotmoka-moka/modules/automatic:target/runs-0.0.1.jar -classpath ../../hotmoka/io-hotmoka-moka/modules/unnamed"/*" --add-modules org.glassfish.tyrus.container.grizzly.server,org.glassfish.tyrus.container.grizzly.client --module runs/runs.Family)
cd ../../hotmoka/tutorial
CODE_FAMILY_ADDRESS=${RUN: -64}
echo "  family-0.0.1.jar address = $CODE_FAMILY_ADDRESS"
sed -i "/@code_family_address/s/\/.*\//\/@code_family_address\/$CODE_FAMILY_ADDRESS\//" $SCRIPT

message "Creating an instance of class \"Person\" (will fail)"
moka create io.takamaka.family.Person "Albert Einstein" 14 4 1879 null null --payer=$ACCOUNT1 --classpath=$FAMILY_ADDRESS --uri=$NETWORK_URI --password-of-payer=chocolate --interactive=false >/dev/null

message "Packaging the \"family_storage\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/family_storage/pom.xml clean package 2>/dev/null

message "Installing \"family_storage-0.0.1.jar\""
FAMILY2_INSTALLATION=$(moka install ../../hotmoka_tutorial/family_storage/target/family_storage-0.0.1.jar --payer=$ACCOUNT1 --uri=$NETWORK_URI --password-of-payer=chocolate --interactive=false)
LINE1=$(echo "$FAMILY2_INSTALLATION"| sed '1!d')
FAMILY2_ADDRESS=${LINE1: -64}
echo "  family_storage-0.0.1.jar address = $FAMILY2_ADDRESS"
sed -i "/@family2_address/s/\/.*\//\/@family2_address\/$FAMILY2_ADDRESS\//" $SCRIPT

message "Creating an instance of class \"Person\""
RUN=$(moka create io.takamaka.family.Person "Albert Einstein" 14 4 1879 null null --payer=$ACCOUNT1 --classpath=$FAMILY2_ADDRESS --uri=$NETWORK_URI --password-of-payer=chocolate --interactive=false)
LINE1=$(echo "$RUN"| sed '1!d')
PERSON_OBJECT=${LINE1: -66}
echo "  Person instance address = $PERSON_OBJECT"
sed -i "/@person_object/s/\/.*\//\/@person_object\/$PERSON_OBJECT\//" $SCRIPT

message "Editing the \"Family2.java\" run example from the tutorial"
sed -i '/ADDRESS = /s/".*"/"'$ACCOUNT1'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Family2.java
sed -i '/URI.create(/s/".*"/"ws:\/\/'$NETWORK_URI_WITHOUT_PROTOCOL'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Family2.java

message "Packaging the \"runs\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/runs/pom.xml package 2>/dev/null

message "Running the \"Family2.java\" run example from the tutorial"
cd ../../hotmoka_tutorial/runs
RUN=$(java --module-path ../../hotmoka/io-hotmoka-moka/modules/explicit/:../../hotmoka/io-hotmoka-moka/modules/automatic:target/runs-0.0.1.jar -classpath ../../hotmoka/io-hotmoka-moka/modules/unnamed"/*" --add-modules org.glassfish.tyrus.container.grizzly.server,org.glassfish.tyrus.container.grizzly.client --module runs/runs.Family2)
cd ../../hotmoka/tutorial
PERSON2_OBJECT=${RUN: -66}
echo "  Person2 instance address = $PERSON2_OBJECT"
sed -i "/@person2_object/s/\/.*\//\/@person2_object\/$PERSON2_OBJECT\//" $SCRIPT

message "Calling method \"toString\" on the \"Person\" object (will fail)"
moka call $PERSON_OBJECT toString --payer=$ACCOUNT1 --uri=$NETWORK_URI --password-of-payer=chocolate --interactive=false >/dev/null

message "Packaging the \"family_exported\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/family_exported/pom.xml clean package 2>/dev/null

message "Installing \"family_exported-0.0.1.jar\""
FAMILY3_INSTALLATION=$(moka install ../../hotmoka_tutorial/family_exported/target/family_exported-0.0.1.jar --payer=$ACCOUNT1 --uri=$NETWORK_URI --password-of-payer=chocolate --interactive=false)
LINE1=$(echo "$FAMILY3_INSTALLATION"| sed '1!d')
FAMILY_EXPORTED_ADDRESS=${LINE1: -64}
echo "  family_exported-0.0.1.jar address = $FAMILY_EXPORTED_ADDRESS"
sed -i "/@family_exported_address/s/\/.*\//\/@family_exported_address\/$FAMILY_EXPORTED_ADDRESS\//" $SCRIPT

message "Creating an instance of class \"Person\""
RUN=$(moka create io.takamaka.family.Person "Albert Einstein" 14 4 1879 null null --payer=$ACCOUNT1 --classpath=$FAMILY_EXPORTED_ADDRESS --uri=$NETWORK_URI --password-of-payer=chocolate --interactive=false)
LINE1=$(echo "$RUN"| sed '1!d')
PERSON3_OBJECT=${LINE1: -66}
echo "  Person instance address = $PERSON3_OBJECT"
sed -i "/@person3_object/s/\/.*\//\/@person3_object\/$PERSON3_OBJECT\//" $SCRIPT

message "Calling method \"toString\" on the last \"Person\" object"
moka call $PERSON3_OBJECT toString --payer=$ACCOUNT1 --uri=$NETWORK_URI --password-of-payer=chocolate --interactive=false >/dev/null

message "Editing the \"Family3.java\" run example from the tutorial"
sed -i '/ADDRESS = /s/".*"/"'$ACCOUNT1'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Family3.java
sed -i '/URI.create(/s/".*"/"ws:\/\/'$NETWORK_URI_WITHOUT_PROTOCOL'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Family3.java

message "Packaging the \"runs\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/runs/pom.xml package 2>/dev/null

message "Running the \"Family3.java\" run example from the tutorial"
cd ../../hotmoka_tutorial/runs
java --module-path ../../hotmoka/io-hotmoka-moka/modules/explicit/:../../hotmoka/io-hotmoka-moka/modules/automatic:target/runs-0.0.1.jar -classpath ../../hotmoka/io-hotmoka-moka/modules/unnamed"/*" --add-modules org.glassfish.tyrus.container.grizzly.server,org.glassfish.tyrus.container.grizzly.client --module runs/runs.Family3
cd ../../hotmoka/tutorial

message "Packaging the \"ponzi_gradual\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/ponzi_gradual/pom.xml clean package 2>/dev/null

message "Installing \"ponzi_gradual-0.0.1.jar\""
PONZI_GRADUAL_INSTALLATION=$(moka install ../../hotmoka_tutorial/ponzi_gradual/target/ponzi_gradual-0.0.1.jar --payer=$ACCOUNT1 --uri=$NETWORK_URI --password-of-payer=chocolate --interactive=false)
LINE1=$(echo "$PONZI_GRADUAL_INSTALLATION"| sed '1!d')
GRADUAL_PONZI_ADDRESS=${LINE1: -64}
echo "  ponzi_gradual-0.0.1.jar address = $GRADUAL_PONZI_ADDRESS"
sed -i "/@gradual_ponzi_address/s/\/.*\//\/@gradual_ponzi_address\/$GRADUAL_PONZI_ADDRESS\//" $SCRIPT

message "Creating account 2 and account 3"
ACCOUNT2_CREATION=$(moka create-account 10000000 --payer $ACCOUNT1 --uri=$NETWORK_URI --password-of-payer=chocolate --password-of-new-account=orange --interactive=false --print-costs=false)
LINE1=$(echo "$ACCOUNT2_CREATION"| sed '1!d')
ACCOUNT2=${LINE1:14:66}
echo "  Account 2 = $ACCOUNT2"
sed -i '/@account2/s/\/.*\//\/@account2\/'$ACCOUNT2'\//' $SCRIPT
ACCOUNT3_CREATION=$(moka create-account 10000000 --payer $ACCOUNT1 --uri=$NETWORK_URI --password-of-payer=chocolate --password-of-new-account=apple --interactive=false --print-costs=false)
LINE1=$(echo "$ACCOUNT3_CREATION"| sed '1!d')
ACCOUNT3=${LINE1:14:66}
echo "  Account 3 = $ACCOUNT3"
sed -i '/@account3/s/\/.*\//\/@account3\/'$ACCOUNT3'\//' $SCRIPT

message "Creating an instance of class \"GradualPonzi\""
RUN=$(moka create io.takamaka.ponzi.GradualPonzi --payer=$ACCOUNT1 --classpath $GRADUAL_PONZI_ADDRESS --uri=$NETWORK_URI --password-of-payer=chocolate --interactive=false)
LINE1=$(echo "$RUN"| sed '1!d')
GRADUAL_PONZI_OBJECT=${LINE1: -66}
echo "  GradualPonzi instance address = $GRADUAL_PONZI_OBJECT"
sed -i "/@gradual_ponzi_object/s/\/.*\//\/@gradual_ponzi_object\/$GRADUAL_PONZI_OBJECT\//" $SCRIPT

message "Account 2 and account 3 invest in the GradualPonzi instance"
moka call $GRADUAL_PONZI_OBJECT invest 5000 --payer $ACCOUNT2 --uri=$NETWORK_URI --password-of-payer=orange --interactive=false >/dev/null
moka call $GRADUAL_PONZI_OBJECT invest 15000 --payer $ACCOUNT3 --uri=$NETWORK_URI --password-of-payer=apple --interactive=false >/dev/null

message "Account 1 invests too little in the GradualPonzi (will fail)"
moka call $GRADUAL_PONZI_OBJECT invest 500 --payer $ACCOUNT1 --uri=$NETWORK_URI --password-of-payer=chocolate --interactive=false >/dev/null

message "Checking the state of the GradualPonzi contract"
RUN=$(moka state $GRADUAL_PONZI_OBJECT --uri=$NETWORK_URI)
LINE=$(echo "$RUN"|tail -3|sed '1!d')
GRADUAL_PONZI_LIST=${LINE: -66}
echo "  GradualPonzi list address = $GRADUAL_PONZI_LIST"
sed -i "/@gradual_ponzi_list/s/\/.*\//\/@gradual_ponzi_list\/$GRADUAL_PONZI_LIST\//" $SCRIPT

message "Checking the state of the list of investors"
RUN=$(moka state $GRADUAL_PONZI_LIST --uri=$NETWORK_URI)
LINE=$(echo "$RUN"|tail -3|sed '1!d')
GRADUAL_PONZI_FIRST=${LINE: -66}
LINE=$(echo "$RUN"|tail -2|sed '1!d')
GRADUAL_PONZI_LAST=${LINE: -66}
echo "  First investor address = $GRADUAL_PONZI_FIRST"
echo "  Last investor address = $GRADUAL_PONZI_LAST"
sed -i "/@gradual_ponzi_first/s/\/.*\//\/@gradual_ponzi_first\/$GRADUAL_PONZI_FIRST\//" $SCRIPT
sed -i "/@gradual_ponzi_last/s/\/.*\//\/@gradual_ponzi_last\/$GRADUAL_PONZI_LAST\//" $SCRIPT

message "Packaging the \"tictactoe_improved\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/tictactoe_improved/pom.xml clean package 2>/dev/null

message "Installing \"tictactoe_improved-0.0.1.jar\""
TICTACTOE_INSTALLATION=$(moka install ../../hotmoka_tutorial/tictactoe_improved/target/tictactoe_improved-0.0.1.jar --payer=$ACCOUNT1 --uri=$NETWORK_URI --password-of-payer=chocolate --interactive=false)
LINE1=$(echo "$TICTACTOE_INSTALLATION"| sed '1!d')
TICTACTOE_ADDRESS=${LINE1: -64}
echo "  tictactoe_improved-0.0.1.jar address = $TICTACTOE_ADDRESS"
sed -i "/@tictactoe_address/s/\/.*\//\/@tictactoe_address\/$TICTACTOE_ADDRESS\//" $SCRIPT

message "Creating an instance of class \"TicTacToe\""
RUN=$(moka create io.takamaka.tictactoe.TicTacToe --payer=$ACCOUNT1 --classpath $TICTACTOE_ADDRESS --uri=$NETWORK_URI --password-of-payer=chocolate --interactive=false)
LINE1=$(echo "$RUN"| sed '1!d')
TICTACTOE_OBJECT=${LINE1: -66}
echo "  TicTacToe instance address = $TICTACTOE_OBJECT"
sed -i "/@tictactoe_object/s/\/.*\//\/@tictactoe_object\/$TICTACTOE_OBJECT\//" $SCRIPT

message "Account 1 and account 2 play tic-tac-toe"
moka call $TICTACTOE_OBJECT play 100 1 1 --payer $ACCOUNT1 --uri=$NETWORK_URI --password-of-payer=chocolate --interactive=false >/dev/null
moka call $TICTACTOE_OBJECT toString --payer $ACCOUNT1 --uri=$NETWORK_URI --print-costs=false
echo
moka call $TICTACTOE_OBJECT play 100 2 1 --payer $ACCOUNT2 --uri=$NETWORK_URI --password-of-payer=orange --interactive=false >/dev/null
moka call $TICTACTOE_OBJECT toString --payer $ACCOUNT2 --uri=$NETWORK_URI --print-costs=false
echo
moka call $TICTACTOE_OBJECT play 0 1 2 --payer $ACCOUNT1 --uri=$NETWORK_URI --password-of-payer=chocolate --interactive=false >/dev/null
moka call $TICTACTOE_OBJECT toString --payer $ACCOUNT1 --uri=$NETWORK_URI --print-costs=false
echo
moka call $TICTACTOE_OBJECT play 0 2 2 --payer $ACCOUNT2 --uri=$NETWORK_URI --password-of-payer=orange --interactive=false >/dev/null
moka call $TICTACTOE_OBJECT toString --payer $ACCOUNT2 --uri=$NETWORK_URI --print-costs=false
echo
moka call $TICTACTOE_OBJECT play 0 1 3 --payer $ACCOUNT1 --uri=$NETWORK_URI --password-of-payer=chocolate --interactive=false >/dev/null
moka call $TICTACTOE_OBJECT toString --payer $ACCOUNT1 --uri=$NETWORK_URI --print-costs=false

message "Account 2 plays tic-tac-toe but it's over (will fail)"
moka call $TICTACTOE_OBJECT play 0 2 3 --payer $ACCOUNT2 --uri=$NETWORK_URI --password-of-payer=orange --interactive=false >/dev/null

message "Creating account 4"
ACCOUNT4_CREATION=$(moka create-account 1000000000000 --payer faucet --uri=$NETWORK_URI --password-of-new-account=game --interactive=false)
LINE2=$(echo "$ACCOUNT4_CREATION"| sed '2!d')
ACCOUNT4=${LINE2:14:66}
echo "  Account 4 = $ACCOUNT4"
sed -i '/@account4/s/\/.*\//\/@account4\/'$ACCOUNT4'\//' $SCRIPT
PUBLICKEYACCOUNT4=$(moka call $ACCOUNT4 publicKey --uri=$NETWORK_URI --print-costs=false --use-colors=false)
SHORT_PUBLICKEYACCOUNT4=${PUBLICKEYACCOUNT4:0:20}...
# we replace the / character of Base64 encodings with the (escaped) escape sequence \/ for "sed"
PUBLICKEYACCOUNT4=$(echo "$PUBLICKEYACCOUNT4" | sed -r 's/\//\\\\\\\//g')
SHORT_PUBLICKEYACCOUNT4=$(echo "$SHORT_PUBLICKEYACCOUNT4" | sed -r 's/\//\\\\\\\//g')
echo "  Public key of account 4 = $PUBLICKEYACCOUNT4"
echo "  Public key of account 4 short = $SHORT_PUBLICKEYACCOUNT4"
sed -i "/@publickeyaccount4/s/\/.*\//\/@publickeyaccount4\/$PUBLICKEYACCOUNT4\//" $SCRIPT
sed -i "/@short_publickeyaccount4/s/\/.*\//\/@short_publickeyaccount4\/$SHORT_PUBLICKEYACCOUNT4\//" $SCRIPT

message "Creating account 5"
ACCOUNT5_CREATION=$(moka create-account 1000000000000 --payer faucet --signature sha256dsa --uri=$NETWORK_URI --password-of-new-account=play --interactive=false)
LINE2=$(echo "$ACCOUNT5_CREATION"| sed '2!d')
ACCOUNT5=${LINE2:14:66}
echo "  Account 5 = $ACCOUNT5"
sed -i '/@account5/s/\/.*\//\/@account5\/'$ACCOUNT5'\//' $SCRIPT
PUBLICKEYACCOUNT5=$(moka call $ACCOUNT5 publicKey --uri=$NETWORK_URI --print-costs=false --use-colors=false)
SHORT_PUBLICKEYACCOUNT5=${PUBLICKEYACCOUNT5:0:30}...
# we replace the / character of Base64 encodings with the (escaped) escape sequence \/ for "sed"
PUBLICKEYACCOUNT5=$(echo "$PUBLICKEYACCOUNT5" | sed -r 's/\//\\\\\\\//g')
SHORT_PUBLICKEYACCOUNT5=$(echo "$SHORT_PUBLICKEYACCOUNT5" | sed -r 's/\//\\\\\\\//g')
echo "  Public key of account 5 = $PUBLICKEYACCOUNT5"
echo "  Public key of account 5 short = $SHORT_PUBLICKEYACCOUNT5"
sed -i "/@publickeyaccount5/s/\/.*\//\/@publickeyaccount5\/$PUBLICKEYACCOUNT5\//" $SCRIPT
sed -i "/@short_publickeyaccount5/s/\/.*\//\/@short_publickeyaccount5\/$SHORT_PUBLICKEYACCOUNT5\//" $SCRIPT

message "Creating account 6"
ACCOUNT6_CREATION=$(moka create-account 1000000000000 --payer faucet --signature qtesla1 --uri=$NETWORK_URI --password-of-new-account=quantum1 --interactive=false)
LINE2=$(echo "$ACCOUNT6_CREATION"| sed '2!d')
ACCOUNT6=${LINE2:14:66}
echo "  Account 6 = $ACCOUNT6"
sed -i '/@account6/s/\/.*\//\/@account6\/'$ACCOUNT6'\//' $SCRIPT

message "Creating account 7"
# the previous creation is so expensive that it might increase the gas cost and the heuristics of moka will fail: better wait
sleep 10
ACCOUNT7_CREATION=$(moka create-account 100000 --payer $ACCOUNT6 --signature qtesla3 --uri=$NETWORK_URI --password-of-payer=quantum1 --password-of-new-account=quantum3 --interactive=false --print-costs=false)
LINE1=$(echo "$ACCOUNT7_CREATION"| sed '1!d')
ACCOUNT7=${LINE1:14:66}
echo "  Account 7 = $ACCOUNT7"
sed -i '/@account7/s/\/.*\//\/@account7\/'$ACCOUNT7'\//' $SCRIPT

message "Installing \"family_exported-0.0.1.jar\""
# the previous creation is so expensive that it might increase the gas cost and the heuristics of moka will fail: better wait
sleep 25
FAMILY3_INSTALLATION=$(moka install ../../hotmoka_tutorial/family_exported/target/family_exported-0.0.1.jar --payer=$ACCOUNT6 --uri=$NETWORK_URI --password-of-payer=quantum1 --interactive=false)
LINE1=$(echo "$FAMILY3_INSTALLATION"| sed '1!d')
FAMILY3_ADDRESS=${LINE1: -64}
echo "  family_exported-0.0.1.jar address = $FAMILY3_ADDRESS"
sed -i "/@family3_address/s/\/.*\//\/@family3_address\/$FAMILY3_ADDRESS\//" $SCRIPT

message "Packaging the \"erc20\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/erc20/pom.xml clean package 2>/dev/null

message "Installing \"erc20.jar\""
# the previous creation is so expensive that it might increase the gas cost and the heuristics of moka will fail: better wait
sleep 5
ERC20_INSTALLATION=$(moka install ../../hotmoka_tutorial/erc20/target/erc20-0.0.1.jar --payer=$ACCOUNT1 --uri=$NETWORK_URI --password-of-payer=chocolate --interactive=false)
LINE1=$(echo "$ERC20_INSTALLATION"| sed '1!d')
ERC20_ADDRESS=${LINE1: -64}
echo "  erc20-0.0.1.jar address = $ERC20_ADDRESS"
sed -i "/@erc20_address/s/\/.*\//\/@erc20_address\/$ERC20_ADDRESS\//" $SCRIPT

message "Creating an instance of class \"CryptoBuddy\""
RUN=$(moka create io.takamaka.erc20.CryptoBuddy --payer=$ACCOUNT1 --classpath $ERC20_ADDRESS --uri=$NETWORK_URI --password-of-payer=chocolate --interactive=false)
LINE1=$(echo "$RUN"| sed '1!d')
ERC20_OBJECT=${LINE1: -66}
echo "  CryptoBuddy instance address = $ERC20_OBJECT"
sed -i "/@erc20_object/s/\/.*\//\/@erc20_object\/$ERC20_OBJECT\//" $SCRIPT

message "Editing the \"Auction.java\" run example from the tutorial"
sed -i '/ADDRESSES\[0\] = /s/".*"/"'$ACCOUNT1'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Auction.java
sed -i '/ADDRESSES\[1\] = /s/".*"/"'$ACCOUNT2'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Auction.java
sed -i '/ADDRESSES\[2\] = /s/".*"/"'$ACCOUNT3'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Auction.java
sed -i '/URI.create(/s/".*"/"ws:\/\/'$NETWORK_URI_WITHOUT_PROTOCOL'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Auction.java

message "Editing the \"Events.java\" run example from the tutorial"
sed -i '/ADDRESSES\[0\] = /s/".*"/"'$ACCOUNT1'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Events.java
sed -i '/ADDRESSES\[1\] = /s/".*"/"'$ACCOUNT2'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Events.java
sed -i '/ADDRESSES\[2\] = /s/".*"/"'$ACCOUNT3'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Events.java
sed -i '/URI.create(/s/".*"/"ws:\/\/'$NETWORK_URI_WITHOUT_PROTOCOL'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Events.java

message "Packaging the \"auction\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/auction/pom.xml clean package 2>/dev/null

message "Packaging the \"auction_events\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/auction_events/pom.xml clean package 2>/dev/null

message "Packaging the \"runs\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/runs/pom.xml package 2>/dev/null

message "Running the \"Auction.java\" run example from the tutorial"
# we provide the private keys of account2 and account3 so that the run works
cp $ACCOUNT2.pem ../../hotmoka_tutorial/
cp $ACCOUNT3.pem ../../hotmoka_tutorial/
cd ../../hotmoka_tutorial/runs
java --module-path ../../hotmoka/io-hotmoka-moka/modules/explicit/:../../hotmoka/io-hotmoka-moka/modules/automatic:target/runs-0.0.1.jar -classpath ../../hotmoka/io-hotmoka-moka/modules/unnamed"/*" --add-modules org.glassfish.tyrus.container.grizzly.server,org.glassfish.tyrus.container.grizzly.client --module runs/runs.Auction
cd ../../hotmoka/tutorial

message "Running the \"Events.java\" run example from the tutorial"
cd ../../hotmoka_tutorial/runs
java --module-path ../../hotmoka/io-hotmoka-moka/modules/explicit/:../../hotmoka/io-hotmoka-moka/modules/automatic:target/runs-0.0.1.jar -classpath ../../hotmoka/io-hotmoka-moka/modules/unnamed"/*" --add-modules org.glassfish.tyrus.container.grizzly.server,org.glassfish.tyrus.container.grizzly.client --module runs/runs.Events
cd ../../hotmoka/tutorial