#!/bin/bash

# An example of a script that asks to charge an account with
# some cryptocurrency and then let the validator key of a local node
# become the key of a validator account. This script is useful
# if the node on the local machine must become a partner of a blockchain:
# get a first account funded and become a validator.

# Source it as follows (if you want to interact with panarea.hotmoka.io,
# but any node of the same blockchain will do):
# bash <(curl -s https://raw.githubusercontent.com/Hotmoka/hotmoka/master/scripts/buy.sh) hotmoka panarea.hotmoka.io

TYPE=${1:-hotmoka}

TYPE_CAPITALIZED=${TYPE^}
DIR=${TYPE}_node_info
if [ $TYPE = hotmoka ];
then
    NETWORK_URL=${2:-panarea.hotmoka.io}
    GITHUB_ID=Hotmoka
    CLI=moka
else
    NETWORK_URL=${2:-blueknot.vero4chain.it}
    GITHUB_ID=Vero4Chain
    CLI=blue
fi;

VERSION=$(curl --silent http://$NETWORK_URL/get/nodeID| python3 -c "import sys, json; print(json.load(sys.stdin)['version'])")

echo "Buying some crypto and becoming a validator of the $TYPE_CAPITALIZED blockchain at $NETWORK_URL, version $VERSION:"
rm -r $DIR 2>/dev/null
mkdir -m700 $DIR

echo " * downloading the blockchain CLI"
mkdir $DIR/$CLI
cd $DIR/$CLI
wget --quiet https://github.com/${GITHUB_ID}/${TYPE}/releases/download/v${VERSION}/${CLI}_${VERSION}.tar.gz
tar zxf ${CLI}_${VERSION}.tar.gz
cd ../..

echo " * creating the money account"
read -s -p "     choose a password for the money account: " PASSWORD
echo
cd $DIR
MONEY_ACCOUNT_CREATION=$(./${CLI}/${CLI} create-key --password-of-new-key=$PASSWORD --interactive=false)
LINE2=$(echo "$MONEY_ACCOUNT_CREATION"| sed '2!d')
MONEY_ACCOUNT_PUBLIC_KEY_BASE58=${LINE2:19}
read -s -p "     tell the seller to pay anonymously to the key $MONEY_ACCOUNT_PUBLIC_KEY_BASE58 then press ENTER"
echo
BINDING=$(./${CLI}/${CLI} bind-key ${MONEY_ACCOUNT_PUBLIC_KEY_BASE58} --url ${NETWORK_URL})
LINE1=$(echo "$BINDING"| sed '1!d')
MONEY_ACCOUNT_ADDRESS=${LINE1:14:66}
ln -s ${MONEY_ACCOUNT_ADDRESS}.pem money.pem
rm ${MONEY_ACCOUNT_PUBLIC_KEY_BASE58}.pem
./${CLI}/${CLI} show-account $MONEY_ACCOUNT_ADDRESS | tail -36 >${MONEY_ACCOUNT_ADDRESS}_36_words.txt
cd ..

echo " * extracting the pem of the validator key"
cd $DIR
docker cp $TYPE:/home/$TYPE/extract/. .
VALIDATOR_KEY=$(ls *.pem)
cd ..

echo " * cleaning up"
rm -r ${DIR}/${CLI}
#rm $DIR/$MONEY_ACCOUNT_PUBLIC_KEY_BASE58.pem

echo
echo "The pem file of the key to control the node as validator"
echo "(if it will ever be a validator) has been saved in the directory \"${DIR}\"."
echo "Move that directory to the clients that need to control the node and delete it from this server."
echo
echo "The password of the validator key is empty."
