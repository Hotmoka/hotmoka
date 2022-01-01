#!/bin/bash

# This script updates the "create_from_source.sh" script
# so that it reflects the content of a Hotmoka node.
# It is useful after a new node has been deployed, if we want the
# tutorial to reflect the actual content of the node.

# Run for instance this way:
# NETWORK_URL="mynode:myport" ./update_for_new_node.sh

# by default, it reflects the panarea.hotmoka.io node
NETWORK_URL=${NETWORK_URL:=panarea.hotmoka.io}

echo "Updating file create_from_source.sh by replaying its examples"
echo "on the Hotmoka node at $NETWORK_URL."

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

ACCOUNT1_CREATION=$(moka create-account 50000000000 --payer faucet --url=$NETWORK_URL --password-of-new-account=chocolate --non-interactive)
LINE2=$(echo "$ACCOUNT1_CREATION"| sed '2!d')
ACCOUNT1=${LINE2:14:66}
echo "  Account 1 = $ACCOUNT1"
sed -i '/@account1/s/\/.*\//\/@account1\/'$ACCOUNT1'\//' create_from_source.sh
ACCOUNT1_SHORT=${ACCOUNT1:0:11}...#0
echo "  Account 1 short = $ACCOUNT1_SHORT"
sed -i '/@short_account1/s/\/.*\//\/@account1_short\/'$ACCOUNT1_SHORT'\//' create_from_source.sh
ACCOUNT1_36WORDS=$(echo "$ACCOUNT1_CREATION" |tail -36|sed ':a;N;$!ba;s/\n/\\n/g')
echo "  Account 1's 36 words = $ACCOUNT1_36WORDS"
sed -i '/@36words_of_account1/s/\/.*\//\/@account1_36words\/'$ACCOUNT1_36WORDS'\//' create_from_source.sh


