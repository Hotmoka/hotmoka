#!/bin/bash

# An example of a script that asks to charge an account with
# some cryptocurrency and then let the validator key of a local node
# become the key of a validator account. This script is useful
# if the node on the local machine must become a partner of a blockchain:
# get a first account funded and become a validator.

# Source it as follows (if you want to interact with panarea.hotmoka.io,
# but any node of the same blockchain will do):
# bash <(curl -s https://raw.githubusercontent.com/Hotmoka/hotmoka/master/scripts/buy.sh) hotmoka ws://panarea.hotmoka.io

TYPE=${1:-hotmoka}

TYPE_CAPITALIZED=${TYPE^}
DIR=${TYPE}_node_info
if [ $TYPE = hotmoka ];
then
    NETWORK_URI=${2:-ws://panarea.hotmoka.io}
    GITHUB_ID=Hotmoka
    CLI=moka
else
    NETWORK_URI=${2:-ws://blueknot.vero4chain.it}
    GITHUB_ID=Vero4Chain
    CLI=blue
fi;

VERSION=$(moka node info --json --uri $NETWORK_URI | python3 -c "import sys, json; print(json.load(sys.stdin)['version'])")

echo "Buying some crypto and becoming a validator of the $TYPE_CAPITALIZED blockchain at $NETWORK_URI, version $VERSION:"
rm -r $DIR 2>/dev/null
mkdir -m700 $DIR

echo " * extracting the pem of the validator key"
cd $DIR
docker cp ${TYPE}:/home/${TYPE}/extract/. .
VALIDATOR_KEY=$(ls *.pem)
VALIDATOR_KEY_BASE58=${VALIDATOR_KEY::-4}
cd ..

echo " * downloading the blockchain CLI"
mkdir $DIR/${CLI}
cd $DIR/${CLI}
wget --quiet https://github.com/${GITHUB_ID}/${TYPE}/releases/download/v${VERSION}/${CLI}_${VERSION}.tar.gz
tar zxf ${CLI}_${VERSION}.tar.gz
cd ../..

echo " * creating the money account"
read -s -p "     choose a password for the money account: " PASSWORD
echo
cd $DIR
MONEY_ACCOUNT_CREATION=$(./${CLI}/${CLI} create-key --password-of-new-key=${PASSWORD} --interactive=false)
LINE2=$(echo "$MONEY_ACCOUNT_CREATION"| sed '2!d')
MONEY_ACCOUNT_PUBLIC_KEY_BASE58=${LINE2:19}

echo "     tell the seller to pay to the key $MONEY_ACCOUNT_PUBLIC_KEY_BASE58"

while true; do
    echo "       waiting..."
    sleep 10

    BINDING=$(./${CLI}/${CLI} bind-key ${MONEY_ACCOUNT_PUBLIC_KEY_BASE58} --uri ${NETWORK_URI} 2>/dev/null)
    LINE1=$(echo "$BINDING"| sed '1!d')

    if [[ "$LINE1" == *"has been created"* ]];
    then
	break
    fi;
done

MONEY_ACCOUNT_ADDRESS=${LINE1:14:66}
ln -s ${MONEY_ACCOUNT_ADDRESS}.pem money.pem
rm ${MONEY_ACCOUNT_PUBLIC_KEY_BASE58}.pem
./${CLI}/${CLI} show-account ${MONEY_ACCOUNT_ADDRESS} | tail -36 >${MONEY_ACCOUNT_ADDRESS}_36_words.txt
cd ..

echo " * creating the validator account"
cd $DIR
# this account will subsequently accept the validation power sale offer: 1000000 should be enough for that
RUN=$(./${CLI}/${CLI} create-account 1000000 --key-of-new-account ${VALIDATOR_KEY_BASE58} --payer ${MONEY_ACCOUNT_ADDRESS} --password-of-payer=${PASSWORD} --create-tendermint-validator --interactive=false --print-costs=false --uri ${NETWORK_URI})
LINE1=$(echo "$RUN"| sed '1!d')
VALIDATOR_ACCOUNT_ADDRESS=${LINE1:14:66}
rm ${VALIDATOR_KEY_BASE58}.pem
ln -s ${VALIDATOR_ACCOUNT_ADDRESS}.pem validator.pem
./${CLI}/${CLI} show-account ${VALIDATOR_ACCOUNT_ADDRESS} | tail -36 >${VALIDATOR_ACCOUNT_ADDRESS}_36_words.txt
PASSWORD=
cd ..

echo " * accepting a sale offer of validation power"
cd $DIR
echo "     tell the seller to sell validation power to $VALIDATOR_ACCOUNT_ADDRESS"
MANIFEST=$(./${CLI}/${CLI} info --uri ${NETWORK_URI})
LINE=$(echo "$MANIFEST" | grep "validators" | sed '1!d')
VALIDATORS_ADDRESS=${LINE: -66}

while [ true ]
do
    echo "       waiting..."
    sleep 10

    RUN=$(./${CLI}/${CLI} call ${VALIDATORS_ADDRESS} getOffers --use-colors=false --uri ${NETWORK_URI})
    OFFERS_ADDRESS=$(echo "$RUN"| sed '1!d')
    RUN=$(./${CLI}/${CLI} call ${OFFERS_ADDRESS} size --class-of-receiver=io.takamaka.code.util.StorageSetView --use-colors=false --uri ${NETWORK_URI})
    NUM_OFFERS=$(echo "$RUN"| sed '1!d')
    for (( INDEX=0; INDEX<${NUM_OFFERS}; INDEX++ ))
    do
	RUN=$(./${CLI}/${CLI} call ${OFFERS_ADDRESS} select ${INDEX} --class-of-receiver=io.takamaka.code.util.StorageSetView --use-colors=false --uri ${NETWORK_URI})
	OFFER_ADDRESS=$(echo "$RUN"| sed '1!d')
	RUN=$(./${CLI}/${CLI} call ${OFFER_ADDRESS} getBuyer --class-of-receiver=io.takamaka.code.dao.SharedEntity\$Offer --use-colors=false --uri ${NETWORK_URI})
	BUYER_ADDRESS=$(echo "$RUN"| sed '1!d')
	if [[ "$BUYER_ADDRESS" == "$VALIDATOR_ACCOUNT_ADDRESS" ]];
	then
	    RUN=$(./${CLI}/${CLI} call ${OFFER_ADDRESS} getCost --class-of-receiver=io.takamaka.code.dao.SharedEntity\$Offer --use-colors=false --uri ${NETWORK_URI})
	    COST=$(echo "$RUN"| sed '1!d')
	    if [[ "$COST" == "0" ]];
	    then
		./${CLI}/${CLI} buy-validation ${VALIDATOR_ACCOUNT_ADDRESS} ${OFFER_ADDRESS} --password-of-buyer= --interactive=false --print-costs=false --uri ${NETWORK_URI} >/dev/null
		break 2
	    fi;
	fi;
    done
done
cd ..

echo " * cleaning up"
rm -r ${DIR}/${CLI}

echo
echo "The pem files to control the money account and the validator have been saved in the directory \"${DIR}\"."
echo "Move that directory to the clients that need to control the node and delete it from this server."
echo
echo "The password of the money account is what you have chosen above."
echo "The password of the validator is empty."
