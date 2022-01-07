#!/bin/bash

# This script updates the "create_from_source.sh" script
# so that it reflects the content of a Hotmoka node.
# It is useful after a new node has been deployed, if we want the
# documentation and the tutorail examples
# to reflect the actual content of the node.

# Run for instance this way:
# NETWORK_URL="mynode:myport" ./update_for_new_node.sh

# by default, it reflects the panarea.hotmoka.io node
NETWORK_URL=${NETWORK_URL:=panarea.hotmoka.io}
RED='\033[1;31m'
NC='\033[0m'
message() {
    printf "${RED}$@${NC}\n"
}

message "Updating file create_from_source.sh by replaying its examples on the Hotmoka node at ${NETWORK_URL}"

echo "  Server = $NETWORK_URL"
sed -i '/@server/s/\/.*\//\/@server\/'$NETWORK_URL'\//' create_from_source.sh
VERSION=$(curl --silent http://$NETWORK_URL/get/nodeID| python3 -c "import sys, json; print(json.load(sys.stdin)['version'])")
echo "  Hotmoka version = $VERSION"
sed -i '/@hotmoka_version/s/\/.*\//\/@hotmoka_version\/'$VERSION'\//' create_from_source.sh

TAKAMAKA_CODE=$(curl --silent http://$NETWORK_URL/get/takamakaCode| python3 -c "import sys, json; print(json.load(sys.stdin)['hash'])")
echo "  Takamaka code = $TAKAMAKA_CODE"
sed -i '/@takamakaCode/s/\/.*\//\/@takamakaCode\/'$TAKAMAKA_CODE'\//' create_from_source.sh

MANIFEST_TRANSACTION=$(curl --silent http://$NETWORK_URL/get/manifest| python3 -c "import sys, json; print(json.load(sys.stdin)['transaction']['hash'])")
MANIFEST_PROGRESSIVE=$(curl --silent http://$NETWORK_URL/get/manifest| python3 -c "import sys, json; print(json.load(sys.stdin)['progressive'])")
MANIFEST=$MANIFEST_TRANSACTION#$MANIFEST_PROGRESSIVE
echo "  Manifest = $MANIFEST"
sed -i '/@manifest/s/\/.*\//\/@manifest\/'$MANIFEST'\//' create_from_source.sh

GAMETE=$(moka call $MANIFEST getGamete --url=$NETWORK_URL --print-costs=false --use-colors=false)
echo "  Gamete = $GAMETE"
sed -i '/@gamete/s/\/.*\//\/@gamete\/'$GAMETE'\//' create_from_source.sh

GAS_STATION=$(moka call $MANIFEST getGasStation --url=$NETWORK_URL --print-costs=false --use-colors=false)
echo "  Gas Station = $GAS_STATION"
sed -i '/@gasStation/s/\/.*\//\/@gasStation\/'$GAS_STATION'\//' create_from_source.sh

VALIDATORS=$(moka call $MANIFEST getValidators --url=$NETWORK_URL --print-costs=false --use-colors=false)
echo "  Validators = $VALIDATORS"
sed -i '/@validators/s/\/.*\//\/@validators\/'$VALIDATORS'\//' create_from_source.sh

MAX_FAUCET=$(moka call $GAMETE getMaxFaucet --url=$NETWORK_URL --print-costs=false --use-colors=false)
echo "  Max faucet = $MAX_FAUCET"
sed -i '/@maxFaucet/s/\/.*\//\/@maxFaucet\/'$MAX_FAUCET'\//' create_from_source.sh

CHAIN_ID=$(moka call $MANIFEST getChainId --url=$NETWORK_URL --print-costs=false --use-colors=false)
echo "  Chain ID = $CHAIN_ID"
sed -i '/@chainid/s/\/.*\//\/@chainid\/'$CHAIN_ID'\//' create_from_source.sh

message "Creating account 1"
ACCOUNT1_CREATION=$(moka create-account 50000000000 --payer faucet --url=$NETWORK_URL --password-of-new-account=chocolate --interactive=false)
LINE2=$(echo "$ACCOUNT1_CREATION"| sed '2!d')
ACCOUNT1=${LINE2:14:66}
echo "  Account 1 = $ACCOUNT1"
sed -i '/@account1/s/\/.*\//\/@account1\/'$ACCOUNT1'\//' create_from_source.sh
ACCOUNT1_SHORT=${ACCOUNT1:0:11}...#0
echo "  Account 1 short = $ACCOUNT1_SHORT"
sed -i '/@short_account1/s/\/.*\//\/@short_account1\/'$ACCOUNT1_SHORT'\//' create_from_source.sh
# we replace the new line with the string \\n (ie, escaped \n)
ACCOUNT1_36WORDS=$(echo "$ACCOUNT1_CREATION" |tail -36|sed ':a;N;$!ba;s/\n/\\\\n/g')
echo "  Account 1's 36 words = $ACCOUNT1_36WORDS"
sed -i "/@36words_of_account1/s/\/.*\//\/@36words_of_account1\/$ACCOUNT1_36WORDS\//" create_from_source.sh

PUBLICKEYACCOUNT1=$(moka call $ACCOUNT1 publicKey --url=$NETWORK_URL --print-costs=false --use-colors=false)
SHORT_PUBLICKEYACCOUNT1=${PUBLICKEYACCOUNT1:0:10}...
# we replace the / character of Base64 encodings with the (escaped) escape sequence \/ for "sed"
PUBLICKEYACCOUNT1=$(echo "$PUBLICKEYACCOUNT1" | sed -r 's/\//\\\\\\\//g')
SHORT_PUBLICKEYACCOUNT1=$(echo "$SHORT_PUBLICKEYACCOUNT1" | sed -r 's/\//\\\\\\\//g')
echo "  Public key of account 1 = $PUBLICKEYACCOUNT1"
echo "  Public key of account 1 short = $SHORT_PUBLICKEYACCOUNT1"
sed -i "/@publickeyaccount1/s/\/.*\//\/@publickeyaccount1\/$PUBLICKEYACCOUNT1\//" create_from_source.sh
sed -i "/@short_publickeyaccount1/s/\/.*\//\/@short_publickeyaccount1\/$SHORT_PUBLICKEYACCOUNT1\//" create_from_source.sh

message "Recharging account 1"

moka send 200000 $ACCOUNT1 --payer faucet --url=$NETWORK_URL --print-costs=false --interactive=false

message "Sending coins to an anonymous key"
RUN=$(moka create-key --password-of-new-key=kiwis --interactive=false)
LINE1=$(echo "$RUN"| sed '1!d')
LINE1=${LINE1:10}
NEW_KEY=${LINE1::-18}
echo "  new key = $NEW_KEY"
sed -i "/@new_key/s/\/.*\//\/@new_key\/$NEW_KEY\//" create_from_source.sh
moka send 10000 $NEW_KEY --anonymous --payer=$ACCOUNT1 --url=$NETWORK_URL --password-of-payer=chocolate --interactive=false --print-costs=false >/dev/null
RUN=$(moka bind-key $NEW_KEY --url $NETWORK_URL)
LINE1=$(echo "$RUN"| sed '1!d')
ACCOUNT_ANONYMOUS=${LINE2:14:66}
echo "  anonymous account = $ACCOUNT_ANONYMOUS"
sed -i "/@account_anonymous/s/\/.*\//\/@account_anonymous\/$ACCOUNT_ANONYMOUS\//" create_from_source.sh

message "Packaging the \"family\" example from the tutorial"
# It assumes the tutorial is in a sibling directory of this project
mvn -q -f ../../hotmoka_tutorial/family/pom.xml clean package 2>/dev/null

message "Installing \"family-0.0.1.jar\""
FAMILY_INSTALLATION=$(moka install ../../hotmoka_tutorial/family/target/family-0.0.1.jar --payer=$ACCOUNT1 --url=$NETWORK_URL --password-of-payer=chocolate --interactive=false)
LINE1=$(echo "$FAMILY_INSTALLATION"| sed '1!d')
FAMILY_ADDRESS=${LINE1: -64}
echo "  family-0.0.1.jar address = $FAMILY_ADDRESS"
sed -i "/@family_address/s/\/.*\//\/@family_address\/$FAMILY_ADDRESS\//" create_from_source.sh
SHORT_FAMILY_ADDRESS=${FAMILY_ADDRESS:0:10}...
sed -i "/@short_family_address/s/\/.*\//\/@short_family_address\/$SHORT_FAMILY_ADDRESS\//" create_from_source.sh

message "Editing the \"Family.java\" run example from the tutorial"
sed -i '/ADDRESS = /s/".*"/"'$ACCOUNT1'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Family.java
sed -i '/setURL(/s/".*"/"'$NETWORK_URL'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Family.java

message "Packaging the \"runs\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/runs/pom.xml package 2>/dev/null

message "Running the \"Family.java\" run example from the tutorial"
# we provide the private key of account1 so that the run works
cp $ACCOUNT1.pem ../../hotmoka_tutorial/
cd ../../hotmoka_tutorial/runs
RUN=$(java --module-path ../../hotmoka/modules/explicit/:../../hotmoka/modules/automatic:target/runs-0.0.1.jar -classpath ../../hotmoka/modules/unnamed"/*" --module runs/runs.Family)
cd ../../hotmoka/tutorial
CODE_FAMILY_ADDRESS=${RUN: -64}
echo "  family-0.0.1.jar address = $CODE_FAMILY_ADDRESS"
sed -i "/@code_family_address/s/\/.*\//\/@code_family_address\/$CODE_FAMILY_ADDRESS\//" create_from_source.sh

message "Creating an instance of class \"Person\" (will fail)"
moka create io.takamaka.family.Person "Albert Einstein" 14 4 1879 null null --payer=$ACCOUNT1 --classpath=$FAMILY_ADDRESS --url=$NETWORK_URL --password-of-payer=chocolate --interactive=false >/dev/null

message "Packaging the \"family_storage\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/family_storage/pom.xml clean package 2>/dev/null

message "Installing \"family_storage-0.0.1.jar\""
FAMILY2_INSTALLATION=$(moka install ../../hotmoka_tutorial/family_storage/target/family_storage-0.0.1.jar --payer=$ACCOUNT1 --url=$NETWORK_URL --password-of-payer=chocolate --interactive=false)
LINE1=$(echo "$FAMILY2_INSTALLATION"| sed '1!d')
FAMILY2_ADDRESS=${LINE1: -64}
echo "  family_storage-0.0.1.jar address = $FAMILY2_ADDRESS"
sed -i "/@family2_address/s/\/.*\//\/@family2_address\/$FAMILY2_ADDRESS\//" create_from_source.sh

message "Creating an instance of class \"Person\""
RUN=$(moka create io.takamaka.family.Person "Albert Einstein" 14 4 1879 null null --payer=$ACCOUNT1 --classpath=$FAMILY2_ADDRESS --url=$NETWORK_URL --password-of-payer=chocolate --interactive=false)
LINE1=$(echo "$RUN"| sed '1!d')
PERSON_OBJECT=${LINE1: -66}
echo "  Person instance address = $PERSON_OBJECT"
sed -i "/@person_object/s/\/.*\//\/@person_object\/$PERSON_OBJECT\//" create_from_source.sh

message "Editing the \"Family2.java\" run example from the tutorial"
sed -i '/ADDRESS = /s/".*"/"'$ACCOUNT1'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Family2.java
sed -i '/setURL(/s/".*"/"'$NETWORK_URL'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Family2.java

message "Packaging the \"runs\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/runs/pom.xml package 2>/dev/null

message "Running the \"Family2.java\" run example from the tutorial"
cd ../../hotmoka_tutorial/runs
RUN=$(java --module-path ../../hotmoka/modules/explicit/:../../hotmoka/modules/automatic:target/runs-0.0.1.jar -classpath ../../hotmoka/modules/unnamed"/*" --module runs/runs.Family2)
cd ../../hotmoka/tutorial
PERSON2_OBJECT=${RUN: -66}
echo "  Person2 instance address = $PERSON2_OBJECT"
sed -i "/@person2_object/s/\/.*\//\/@person2_object\/$PERSON2_OBJECT\//" create_from_source.sh

message "Calling method \"toString\" on the \"Person\" object (will fail)"
moka call $PERSON_OBJECT toString --payer=$ACCOUNT1 --url=$NETWORK_URL --password-of-payer=chocolate --interactive=false >/dev/null

message "Packaging the \"family_exported\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/family_exported/pom.xml clean package 2>/dev/null

message "Installing \"family_exported-0.0.1.jar\""
FAMILY3_INSTALLATION=$(moka install ../../hotmoka_tutorial/family_exported/target/family_exported-0.0.1.jar --payer=$ACCOUNT1 --url=$NETWORK_URL --password-of-payer=chocolate --interactive=false)
LINE1=$(echo "$FAMILY3_INSTALLATION"| sed '1!d')
FAMILY_EXPORTED_ADDRESS=${LINE1: -64}
echo "  family_exported-0.0.1.jar address = $FAMILY_EXPORTED_ADDRESS"
sed -i "/@family_exported_address/s/\/.*\//\/@family_exported_address\/$FAMILY_EXPORTED_ADDRESS\//" create_from_source.sh

message "Creating an instance of class \"Person\""
RUN=$(moka create io.takamaka.family.Person "Albert Einstein" 14 4 1879 null null --payer=$ACCOUNT1 --classpath=$FAMILY_EXPORTED_ADDRESS --url=$NETWORK_URL --password-of-payer=chocolate --interactive=false)
LINE1=$(echo "$RUN"| sed '1!d')
PERSON3_OBJECT=${LINE1: -66}
echo "  Person instance address = $PERSON3_OBJECT"
sed -i "/@person3_object/s/\/.*\//\/@person3_object\/$PERSON3_OBJECT\//" create_from_source.sh

message "Calling method \"toString\" on the last \"Person\" object"
moka call $PERSON3_OBJECT toString --payer=$ACCOUNT1 --url=$NETWORK_URL --password-of-payer=chocolate --interactive=false >/dev/null

message "Editing the \"Family3.java\" run example from the tutorial"
sed -i '/ADDRESS = /s/".*"/"'$ACCOUNT1'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Family3.java
sed -i '/setURL(/s/".*"/"'$NETWORK_URL'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Family3.java

message "Packaging the \"runs\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/runs/pom.xml package 2>/dev/null

message "Running the \"Family3.java\" run example from the tutorial"
cd ../../hotmoka_tutorial/runs
java --module-path ../../hotmoka/modules/explicit/:../../hotmoka/modules/automatic:target/runs-0.0.1.jar -classpath ../../hotmoka/modules/unnamed"/*" --module runs/runs.Family3
cd ../../hotmoka/tutorial

message "Packaging the \"ponzi_gradual\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/ponzi_gradual/pom.xml clean package 2>/dev/null

message "Installing \"ponzi_gradual-0.0.1.jar\""
PONZI_GRADUAL_INSTALLATION=$(moka install ../../hotmoka_tutorial/ponzi_gradual/target/ponzi_gradual-0.0.1.jar --payer=$ACCOUNT1 --url=$NETWORK_URL --password-of-payer=chocolate --interactive=false)
LINE1=$(echo "$PONZI_GRADUAL_INSTALLATION"| sed '1!d')
GRADUAL_PONZI_ADDRESS=${LINE1: -64}
echo "  ponzi_gradual-0.0.1.jar address = $GRADUAL_PONZI_ADDRESS"
sed -i "/@gradual_ponzi_address/s/\/.*\//\/@gradual_ponzi_address\/$GRADUAL_PONZI_ADDRESS\//" create_from_source.sh

message "Creating account 2 and account 3"
ACCOUNT2_CREATION=$(moka create-account 10000000 --payer $ACCOUNT1 --url=$NETWORK_URL --password-of-payer=chocolate --password-of-new-account=orange --interactive=false --print-costs=false)
LINE1=$(echo "$ACCOUNT2_CREATION"| sed '1!d')
ACCOUNT2=${LINE1:14:66}
echo "  Account 2 = $ACCOUNT2"
sed -i '/@account2/s/\/.*\//\/@account2\/'$ACCOUNT2'\//' create_from_source.sh
ACCOUNT3_CREATION=$(moka create-account 10000000 --payer $ACCOUNT1 --url=$NETWORK_URL --password-of-payer=chocolate --password-of-new-account=apple --interactive=false --print-costs=false)
LINE1=$(echo "$ACCOUNT3_CREATION"| sed '1!d')
ACCOUNT3=${LINE1:14:66}
echo "  Account 3 = $ACCOUNT3"
sed -i '/@account3/s/\/.*\//\/@account3\/'$ACCOUNT3'\//' create_from_source.sh

message "Creating an instance of class \"GradualPonzi\""
RUN=$(moka create io.takamaka.ponzi.GradualPonzi --payer=$ACCOUNT1 --classpath $GRADUAL_PONZI_ADDRESS --url=$NETWORK_URL --password-of-payer=chocolate --interactive=false)
LINE1=$(echo "$RUN"| sed '1!d')
GRADUAL_PONZI_OBJECT=${LINE1: -66}
echo "  GradualPonzi instance address = $GRADUAL_PONZI_OBJECT"
sed -i "/@gradual_ponzi_object/s/\/.*\//\/@gradual_ponzi_object\/$GRADUAL_PONZI_OBJECT\//" create_from_source.sh

message "Account 2 and account 3 invest in the GradualPonzi instance"
moka call $GRADUAL_PONZI_OBJECT invest 5000 --payer $ACCOUNT2 --url=$NETWORK_URL --password-of-payer=orange --interactive=false >/dev/null
moka call $GRADUAL_PONZI_OBJECT invest 15000 --payer $ACCOUNT3 --url=$NETWORK_URL --password-of-payer=apple --interactive=false >/dev/null

message "Account 1 invests too little in the GradualPonzi (will fail)"
moka call $GRADUAL_PONZI_OBJECT invest 500 --payer $ACCOUNT1 --url=$NETWORK_URL --password-of-payer=chocolate --interactive=false >/dev/null

message "Checking the state of the GradualPonzi contract"
RUN=$(moka state $GRADUAL_PONZI_OBJECT --url=$NETWORK_URL)
LINE=$(echo "$RUN"|tail -3|sed '1!d')
GRADUAL_PONZI_LIST=${LINE: -66}
echo "  GradualPonzi list address = $GRADUAL_PONZI_LIST"
sed -i "/@gradual_ponzi_list/s/\/.*\//\/@gradual_ponzi_list\/$GRADUAL_PONZI_LIST\//" create_from_source.sh

message "Checking the state of the list of investors"
RUN=$(moka state $GRADUAL_PONZI_LIST --url=$NETWORK_URL)
LINE=$(echo "$RUN"|tail -3|sed '1!d')
GRADUAL_PONZI_FIRST=${LINE: -66}
LINE=$(echo "$RUN"|tail -2|sed '1!d')
GRADUAL_PONZI_LAST=${LINE: -66}
echo "  First investor address = $GRADUAL_PONZI_FIRST"
echo "  Last investor address = $GRADUAL_PONZI_LAST"
sed -i "/@gradual_ponzi_first/s/\/.*\//\/@gradual_ponzi_first\/$GRADUAL_PONZI_FIRST\//" create_from_source.sh
sed -i "/@gradual_ponzi_last/s/\/.*\//\/@gradual_ponzi_last\/$GRADUAL_PONZI_LAST\//" create_from_source.sh

message "Packaging the \"tictactoe_improved\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/tictactoe_improved/pom.xml clean package 2>/dev/null

message "Installing \"tictactoe_improved-0.0.1.jar\""
TICTACTOE_INSTALLATION=$(moka install ../../hotmoka_tutorial/tictactoe_improved/target/tictactoe_improved-0.0.1.jar --payer=$ACCOUNT1 --url=$NETWORK_URL --password-of-payer=chocolate --interactive=false)
LINE1=$(echo "$TICTACTOE_INSTALLATION"| sed '1!d')
TICTACTOE_ADDRESS=${LINE1: -64}
echo "  tictactoe_improved-0.0.1.jar address = $TICTACTOE_ADDRESS"
sed -i "/@tictactoe_address/s/\/.*\//\/@tictactoe_address\/$TICTACTOE_ADDRESS\//" create_from_source.sh

message "Creating an instance of class \"TicTacToe\""
RUN=$(moka create io.takamaka.tictactoe.TicTacToe --payer=$ACCOUNT1 --classpath $TICTACTOE_ADDRESS --url=$NETWORK_URL --password-of-payer=chocolate --interactive=false)
LINE1=$(echo "$RUN"| sed '1!d')
TICTACTOE_OBJECT=${LINE1: -66}
echo "  TicTacToe instance address = $TICTACTOE_OBJECT"
sed -i "/@tictactoe_object/s/\/.*\//\/@tictactoe_object\/$TICTACTOE_OBJECT\//" create_from_source.sh

message "Account 1 and account 2 play tic-tac-toe"
moka call $TICTACTOE_OBJECT play 100 1 1 --payer $ACCOUNT1 --url=$NETWORK_URL --password-of-payer=chocolate --interactive=false >/dev/null
moka call $TICTACTOE_OBJECT toString --payer $ACCOUNT1 --url=$NETWORK_URL --print-costs=false
echo
moka call $TICTACTOE_OBJECT play 100 2 1 --payer $ACCOUNT2 --url=$NETWORK_URL --password-of-payer=orange --interactive=false >/dev/null
moka call $TICTACTOE_OBJECT toString --payer $ACCOUNT2 --url=$NETWORK_URL --print-costs=false
echo
moka call $TICTACTOE_OBJECT play 0 1 2 --payer $ACCOUNT1 --url=$NETWORK_URL --password-of-payer=chocolate --interactive=false >/dev/null
moka call $TICTACTOE_OBJECT toString --payer $ACCOUNT1 --url=$NETWORK_URL --print-costs=false
echo
moka call $TICTACTOE_OBJECT play 0 2 2 --payer $ACCOUNT2 --url=$NETWORK_URL --password-of-payer=orange --interactive=false >/dev/null
moka call $TICTACTOE_OBJECT toString --payer $ACCOUNT2 --url=$NETWORK_URL --print-costs=false
echo
moka call $TICTACTOE_OBJECT play 0 1 3 --payer $ACCOUNT1 --url=$NETWORK_URL --password-of-payer=chocolate --interactive=false >/dev/null
moka call $TICTACTOE_OBJECT toString --payer $ACCOUNT1 --url=$NETWORK_URL --print-costs=false

message "Account 2 plays tic-tac-toe but it's over (will fail)"
moka call $TICTACTOE_OBJECT play 0 2 3 --payer $ACCOUNT2 --url=$NETWORK_URL --password-of-payer=orange --interactive=false >/dev/null

message "Creating account 4"
ACCOUNT4_CREATION=$(moka create-account 1000000000000 --payer faucet --url=$NETWORK_URL --password-of-new-account=game --interactive=false)
LINE2=$(echo "$ACCOUNT4_CREATION"| sed '2!d')
ACCOUNT4=${LINE2:14:66}
echo "  Account 4 = $ACCOUNT4"
sed -i '/@account4/s/\/.*\//\/@account4\/'$ACCOUNT4'\//' create_from_source.sh
PUBLICKEYACCOUNT4=$(moka call $ACCOUNT4 publicKey --url=$NETWORK_URL --print-costs=false --use-colors=false)
SHORT_PUBLICKEYACCOUNT4=${PUBLICKEYACCOUNT4:0:20}...
# we replace the / character of Base64 encodings with the (escaped) escape sequence \/ for "sed"
PUBLICKEYACCOUNT4=$(echo "$PUBLICKEYACCOUNT4" | sed -r 's/\//\\\\\\\//g')
SHORT_PUBLICKEYACCOUNT4=$(echo "$SHORT_PUBLICKEYACCOUNT4" | sed -r 's/\//\\\\\\\//g')
echo "  Public key of account 4 = $PUBLICKEYACCOUNT4"
echo "  Public key of account 4 short = $SHORT_PUBLICKEYACCOUNT4"
sed -i "/@publickeyaccount4/s/\/.*\//\/@publickeyaccount4\/$PUBLICKEYACCOUNT4\//" create_from_source.sh
sed -i "/@short_publickeyaccount4/s/\/.*\//\/@short_publickeyaccount4\/$SHORT_PUBLICKEYACCOUNT4\//" create_from_source.sh

message "Creating account 5"
ACCOUNT5_CREATION=$(moka create-account 1000000000000 --payer faucet --signature sha256dsa --url=$NETWORK_URL --password-of-new-account=play --interactive=false)
LINE2=$(echo "$ACCOUNT5_CREATION"| sed '2!d')
ACCOUNT5=${LINE2:14:66}
echo "  Account 5 = $ACCOUNT5"
sed -i '/@account5/s/\/.*\//\/@account5\/'$ACCOUNT5'\//' create_from_source.sh
PUBLICKEYACCOUNT5=$(moka call $ACCOUNT5 publicKey --url=$NETWORK_URL --print-costs=false --use-colors=false)
SHORT_PUBLICKEYACCOUNT5=${PUBLICKEYACCOUNT5:0:30}...
# we replace the / character of Base64 encodings with the (escaped) escape sequence \/ for "sed"
PUBLICKEYACCOUNT5=$(echo "$PUBLICKEYACCOUNT5" | sed -r 's/\//\\\\\\\//g')
SHORT_PUBLICKEYACCOUNT5=$(echo "$SHORT_PUBLICKEYACCOUNT5" | sed -r 's/\//\\\\\\\//g')
echo "  Public key of account 5 = $PUBLICKEYACCOUNT5"
echo "  Public key of account 5 short = $SHORT_PUBLICKEYACCOUNT5"
sed -i "/@publickeyaccount5/s/\/.*\//\/@publickeyaccount5\/$PUBLICKEYACCOUNT5\//" create_from_source.sh
sed -i "/@short_publickeyaccount5/s/\/.*\//\/@short_publickeyaccount5\/$SHORT_PUBLICKEYACCOUNT5\//" create_from_source.sh

message "Creating account 6"
ACCOUNT6_CREATION=$(moka create-account 1000000000000 --payer faucet --signature qtesla1 --url=$NETWORK_URL --password-of-new-account=quantum1 --interactive=false)
LINE2=$(echo "$ACCOUNT6_CREATION"| sed '2!d')
ACCOUNT6=${LINE2:14:66}
echo "  Account 6 = $ACCOUNT6"
sed -i '/@account6/s/\/.*\//\/@account6\/'$ACCOUNT6'\//' create_from_source.sh

message "Creating account 7"
# the previous creation is so expensive that it might increase the gas cost and the heuristics of moka will fail: better wait
sleep 10
ACCOUNT7_CREATION=$(moka create-account 100000 --payer $ACCOUNT6 --signature qtesla3 --url=$NETWORK_URL --password-of-payer=quantum1 --password-of-new-account=quantum3 --interactive=false --print-costs=false)
LINE1=$(echo "$ACCOUNT7_CREATION"| sed '1!d')
ACCOUNT7=${LINE1:14:66}
echo "  Account 7 = $ACCOUNT7"
sed -i '/@account7/s/\/.*\//\/@account7\/'$ACCOUNT7'\//' create_from_source.sh

message "Installing \"family_exported-0.0.1.jar\""
# the previous creation is so expensive that it might increase the gas cost and the heuristics of moka will fail: better wait
sleep 25
FAMILY3_INSTALLATION=$(moka install ../../hotmoka_tutorial/family_exported/target/family_exported-0.0.1.jar --payer=$ACCOUNT6 --url=$NETWORK_URL --password-of-payer=quantum1 --interactive=false)
LINE1=$(echo "$FAMILY3_INSTALLATION"| sed '1!d')
FAMILY3_ADDRESS=${LINE1: -64}
echo "  family_exported-0.0.1.jar address = $FAMILY3_ADDRESS"
sed -i "/@family3_address/s/\/.*\//\/@family3_address\/$FAMILY3_ADDRESS\//" create_from_source.sh

message "Packaging the \"erc20\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/erc20/pom.xml clean package 2>/dev/null

message "Installing \"erc20.jar\""
# the previous creation is so expensive that it might increase the gas cost and the heuristics of moka will fail: better wait
sleep 5
ERC20_INSTALLATION=$(moka install ../../hotmoka_tutorial/erc20/target/erc20-0.0.1.jar --payer=$ACCOUNT1 --url=$NETWORK_URL --password-of-payer=chocolate --interactive=false)
LINE1=$(echo "$ERC20_INSTALLATION"| sed '1!d')
ERC20_ADDRESS=${LINE1: -64}
echo "  erc20-0.0.1.jar address = $ERC20_ADDRESS"
sed -i "/@erc20_address/s/\/.*\//\/@erc20_address\/$ERC20_ADDRESS\//" create_from_source.sh

message "Creating an instance of class \"CryptoBuddy\""
RUN=$(moka create io.takamaka.erc20.CryptoBuddy --payer=$ACCOUNT1 --classpath $ERC20_ADDRESS --url=$NETWORK_URL --password-of-payer=chocolate --interactive=false)
LINE1=$(echo "$RUN"| sed '1!d')
ERC20_OBJECT=${LINE1: -66}
echo "  CryptoBuddy instance address = $ERC20_OBJECT"
sed -i "/@erc20_object/s/\/.*\//\/@erc20_object\/$ERC20_OBJECT\//" create_from_source.sh

message "Editing the \"Auction.java\" run example from the tutorial"
sed -i '/ADDRESSES\[0\] = /s/".*"/"'$ACCOUNT1'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Auction.java
sed -i '/ADDRESSES\[1\] = /s/".*"/"'$ACCOUNT2'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Auction.java
sed -i '/ADDRESSES\[2\] = /s/".*"/"'$ACCOUNT3'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Auction.java
sed -i '/setURL(/s/".*"/"'$NETWORK_URL'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Auction.java

message "Editing the \"Events.java\" run example from the tutorial"
sed -i '/ADDRESSES\[0\] = /s/".*"/"'$ACCOUNT1'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Events.java
sed -i '/ADDRESSES\[1\] = /s/".*"/"'$ACCOUNT2'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Events.java
sed -i '/ADDRESSES\[2\] = /s/".*"/"'$ACCOUNT3'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Events.java
sed -i '/setURL(/s/".*"/"'$NETWORK_URL'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Events.java

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
java --module-path ../../hotmoka/modules/explicit/:../../hotmoka/modules/automatic:target/runs-0.0.1.jar -classpath ../../hotmoka/modules/unnamed"/*" --module runs/runs.Auction
cd ../../hotmoka/tutorial

message "Running the \"Events.java\" run example from the tutorial"
cd ../../hotmoka_tutorial/runs
java --module-path ../../hotmoka/modules/explicit/:../../hotmoka/modules/automatic:target/runs-0.0.1.jar -classpath ../../hotmoka/modules/unnamed"/*" --module runs/runs.Events
cd ../../hotmoka/tutorial

