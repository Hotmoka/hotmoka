#!/bin/bash

# This script updates the "create_from_source.sh" script
# so that it reflects the content of a Hotmoka node.
# It is useful after a new node has been deployed, if we want the
# tutorial to reflect the actual content of the node.

# Run for instance this way:
# NETWORK_URL="mynode:myport" ./update_for_new_node.sh

# by default, it reflects the panarea.hotmoka.io node
NETWORK_URL=${NETWORK_URL:=panarea.hotmoka.io}

echo "Updating file create_from_source.sh by replaying its examples on the Hotmoka node at $NETWORK_URL."

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

echo "Creating account 1"

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

echo "Recharging account 1"

moka send 200000 $ACCOUNT1 --payer faucet --url=$NETWORK_URL --print-costs=false --non-interactive

echo "Packaging the \"family\" example from the tutorial"
# It assumes the tutorial is in a sibling directory of this project
mvn -q -f ../../hotmoka_tutorial/family/pom.xml package 2>/dev/null

echo "Installing \"family-0.0.1.jar\""
FAMILY_INSTALLATION=$(moka install $ACCOUNT1 ../../hotmoka_tutorial/family/target/family-0.0.1.jar --url=$NETWORK_URL --password-of-payer=chocolate --non-interactive)
LINE1=$(echo "$FAMILY_INSTALLATION"| sed '1!d')
FAMILY_ADDRESS=${LINE1: -64}
echo "  family_0.0.1.jar address = $FAMILY_ADDRESS"
sed -i "/@family_address/s/\/.*\//\/@family_address\/$FAMILY_ADDRESS\//" create_from_source.sh
SHORT_FAMILY_ADDRESS=${FAMILY_ADDRESS:0:10}...
sed -i "/@short_family_address/s/\/.*\//\/@short_family_address\/$SHORT_FAMILY_ADDRESS\//" create_from_source.sh

echo "Editing the \"Family.java\" run example from the tutorial"
sed -i '/ADDRESS = /s/".*"/"'$ACCOUNT1'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Family.java
sed -i '/setURL(/s/".*"/"'$NETWORK_URL'"/' ../../hotmoka_tutorial/runs/src/main/java/runs/Family.java

echo "Packaging the \"runs\" example from the tutorial"
mvn -q -f ../../hotmoka_tutorial/runs/pom.xml package 2>/dev/null

echo "Running the \"Family.java\" run example from the tutorial"
# we provide the private key of account1 so that the run works
cp $ACCOUNT1.pem ../../hotmoka_tutorial/
cd ../../hotmoka_tutorial/runs
RUN=$(java --module-path ../../hotmoka/modules/explicit/:../../hotmoka/modules/automatic:target/runs-0.0.1.jar -classpath ../../hotmoka/modules/unnamed"/*" --module runs/runs.Family)
cd ../../hotmoka/tutorial
CODE_FAMILY_ADDRESS=${RUN: -64}
echo "  family_0.0.1.jar address = $CODE_FAMILY_ADDRESS"
sed -i "/@code_family_address/s/\/.*\//\/@code_family_address\/$CODE_FAMILY_ADDRESS\//" create_from_source.sh
