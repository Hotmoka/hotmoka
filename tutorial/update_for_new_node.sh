#!/bin/bash

# This script updates the "create_from_source.sh" script
# so that it reflects the content of a Hotmoka node.
# It is useful after a new node has been deployed, if we want the
# tutorial to reflect the actual content of the node.

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

CHAIN_ID=$(moka call $MANIFEST getChainId --url=$NETWORK_URL --print-costs=false --use-colors=false)
echo "  CHAIN_ID = $CHAIN_ID"
sed -i '/@chainid/s/\/.*\//\/@chainid\/'$CHAIN_ID'\//' create_from_source.sh
