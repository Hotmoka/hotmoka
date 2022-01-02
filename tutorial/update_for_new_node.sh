#!/bin/bash

# This script updates the "create_from_source.sh" script
# so that it reflects the content of a Hotmoka node.
# It is useful after a new node has been deployed, if we want the
# tutorial to reflect the actual content of the node.

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

ACCOUNT1_CREATION=$(moka create-account 50000000000 --payer faucet --url=$NETWORK_URL --password-of-new-account=chocolate --non-interactive)
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

moka send 200000 $ACCOUNT1 --payer faucet --url=$NETWORK_URL --print-costs=false --non-interactive

message "Packaging the \"family\" example from the tutorial"
# It assumes the tutorial is in a sibling directory of this project
mvn -q -f ../../hotmoka_tutorial/family/pom.xml clean package 2>/dev/null

message "Installing \"family-0.0.1.jar\""
FAMILY_INSTALLATION=$(moka install $ACCOUNT1 ../../hotmoka_tutorial/family/target/family-0.0.1.jar --url=$NETWORK_URL --password-of-payer=chocolate --non-interactive)
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
moka create $ACCOUNT1 io.takamaka.family.Person "Albert Einstein" 14 4 1879 null null --classpath=$FAMILY_ADDRESS --url=$NETWORK_URL --password-of-payer=chocolate --non-interactive >/dev/null

message "Packaging the \"family_storage\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/family_storage/pom.xml clean package 2>/dev/null

message "Installing \"family_storage-0.0.1.jar\""
FAMILY2_INSTALLATION=$(moka install $ACCOUNT1 ../../hotmoka_tutorial/family_storage/target/family_storage-0.0.1.jar --url=$NETWORK_URL --password-of-payer=chocolate --non-interactive)
LINE1=$(echo "$FAMILY2_INSTALLATION"| sed '1!d')
FAMILY2_ADDRESS=${LINE1: -64}
echo "  family_storage-0.0.1.jar address = $FAMILY2_ADDRESS"
sed -i "/@family2_address/s/\/.*\//\/@family2_address\/$FAMILY2_ADDRESS\//" create_from_source.sh

message "Creating an instance of class \"Person\""
RUN=$(moka create $ACCOUNT1 io.takamaka.family.Person "Albert Einstein" 14 4 1879 null null --classpath=$FAMILY2_ADDRESS --url=$NETWORK_URL --password-of-payer=chocolate --non-interactive)
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
moka call $PERSON_OBJECT toString --payer=$ACCOUNT1 --url=$NETWORK_URL --password-of-payer=chocolate --non-interactive >/dev/null

message "Packaging the \"family_exported\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/family_exported/pom.xml clean package 2>/dev/null

message "Installing \"family_exported-0.0.1.jar\""
FAMILY3_INSTALLATION=$(moka install $ACCOUNT1 ../../hotmoka_tutorial/family_exported/target/family_exported-0.0.1.jar --url=$NETWORK_URL --password-of-payer=chocolate --non-interactive)
LINE1=$(echo "$FAMILY3_INSTALLATION"| sed '1!d')
FAMILY_EXPORTED_ADDRESS=${LINE1: -64}
echo "  family_exported-0.0.1.jar address = $FAMILY_EXPORTED_ADDRESS"
sed -i "/@family_exported_address/s/\/.*\//\/@family_exported_address\/$FAMILY_EXPORTED_ADDRESS\//" create_from_source.sh

message "Creating an instance of class \"Person\""
RUN=$(moka create $ACCOUNT1 io.takamaka.family.Person "Albert Einstein" 14 4 1879 null null --classpath=$FAMILY_EXPORTED_ADDRESS --url=$NETWORK_URL --password-of-payer=chocolate --non-interactive)
LINE1=$(echo "$RUN"| sed '1!d')
PERSON3_OBJECT=${LINE1: -66}
echo "  Person instance address = $PERSON3_OBJECT"
sed -i "/@person3_object/s/\/.*\//\/@person3_object\/$PERSON3_OBJECT\//" create_from_source.sh

message "Calling method \"toString\" on the last \"Person\" object"
moka call $PERSON3_OBJECT toString --payer=$ACCOUNT1 --url=$NETWORK_URL --password-of-payer=chocolate --non-interactive >/dev/null

message "Editing the \"Family3.java\" run example from the tutorial"
sed -i '/ADDRESS = /s/".*"/"'$ACCOUNT1'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Family3.java
sed -i '/setURL(/s/".*"/"'$NETWORK_URL'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Family3.java

message "Packaging the \"runs\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/runs/pom.xml package 2>/dev/null

message "Running the \"Family3.java\" run example from the tutorial"
cd ../../hotmoka_tutorial/runs
RUN=$(java --module-path ../../hotmoka/modules/explicit/:../../hotmoka/modules/automatic:target/runs-0.0.1.jar -classpath ../../hotmoka/modules/unnamed"/*" --module runs/runs.Family3)
cd ../../hotmoka/tutorial
echo "  $RUN"

message "Packaging the \"ponzi_gradual\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/ponzi_gradual/pom.xml clean package 2>/dev/null

message "Installing \"ponzi_gradual-0.0.1.jar\""
PONZI_GRADUAL_INSTALLATION=$(moka install $ACCOUNT1 ../../hotmoka_tutorial/ponzi_gradual/target/ponzi_gradual-0.0.1.jar --url=$NETWORK_URL --password-of-payer=chocolate --non-interactive)
LINE1=$(echo "$PONZI_GRADUAL_INSTALLATION"| sed '1!d')
GRADUAL_PONZI_ADDRESS=${LINE1: -64}
echo "  ponzi_gradual-0.0.1.jar address = $GRADUAL_PONZI_ADDRESS"
sed -i "/@gradual_ponzi_address/s/\/.*\//\/@gradual_ponzi_address\/$GRADUAL_PONZI_ADDRESS\//" create_from_source.sh

message "Creating account 2 and account 3"
ACCOUNT2_CREATION=$(moka create-account 10000000 --payer $ACCOUNT1 --url=$NETWORK_URL --password-of-payer=chocolate --password-of-new-account=orange --non-interactive --print-costs=false)
LINE1=$(echo "$ACCOUNT2_CREATION"| sed '1!d')
ACCOUNT2=${LINE1:14:66}
echo "  Account 2 = $ACCOUNT2"
sed -i '/@account2/s/\/.*\//\/@account2\/'$ACCOUNT2'\//' create_from_source.sh
ACCOUNT3_CREATION=$(moka create-account 10000000 --payer $ACCOUNT1 --url=$NETWORK_URL --password-of-payer=chocolate --password-of-new-account=apple --non-interactive --print-costs=false)
LINE1=$(echo "$ACCOUNT3_CREATION"| sed '1!d')
ACCOUNT3=${LINE1:14:66}
echo "  Account 3 = $ACCOUNT3"
sed -i '/@account3/s/\/.*\//\/@account3\/'$ACCOUNT3'\//' create_from_source.sh

message "Creating an instance of class \"GradualPonzi\""
RUN=$(moka create $ACCOUNT1 io.takamaka.ponzi.GradualPonzi --classpath $GRADUAL_PONZI_ADDRESS --url=$NETWORK_URL --password-of-payer=chocolate --non-interactive)
LINE1=$(echo "$RUN"| sed '1!d')
GRADUAL_PONZI_OBJECT=${LINE1: -66}
echo "  GradualPonzi instance address = $GRADUAL_PONZI_OBJECT"
sed -i "/@gradual_ponzi_object/s/\/.*\//\/@gradual_ponzi_object\/$GRADUAL_PONZI_OBJECT\//" create_from_source.sh

message "Account 2 and account 3 invest in the GradualPonzi instance"
moka call $GRADUAL_PONZI_OBJECT invest 5000 --payer $ACCOUNT2 --url=$NETWORK_URL --password-of-payer=orange --non-interactive >/dev/null
moka call $GRADUAL_PONZI_OBJECT invest 15000 --payer $ACCOUNT3 --url=$NETWORK_URL --password-of-payer=apple --non-interactive >/dev/null

message "Account 1 invests too little in the GradualPonzi (will fail)"
moka call $GRADUAL_PONZI_OBJECT invest 500 --payer $ACCOUNT1 --url=$NETWORK_URL --password-of-payer=chocolate --non-interactive >/dev/null

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
