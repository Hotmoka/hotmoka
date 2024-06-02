#!/bin/bash

# an example of a script that starts a brand new blockchain
# initially consisting of a single node

# source it as follows (to install version 1.6.0):
# bash <(curl -H 'Cache-Control: no-cache, no-store' -s https://raw.githubusercontent.com/Hotmoka/hotmoka/master/scripts/init.sh) hotmoka 1.6.0
# or (for a test network):
# bash <(curl -H 'Cache-Control: no-cache, no-store' -s https://raw.githubusercontent.com/Hotmoka/hotmoka/master/scripts/init.sh) hotmoka 1.6.0 test

TYPE=${1:-hotmoka}
VERSION=${2:-1.6.0}
TEST=${3:-false}

TYPE_CAPITALIZED=${TYPE^}
DIR=${TYPE}_node_info
if [ $TYPE = hotmoka ];
then
    DOCKER_ID=hotmoka
    CLI=moka
    GITHUB_ID=Hotmoka
    if [ $TEST = false ];
	then
    	CHAIN_ID=salsa
	else
    	CHAIN_ID=marabunta
    fi;
    INITIAL_SUPPLY=10000000000000000000000000000000000
else
    DOCKER_ID=veroforchain
    CLI=blue
    GITHUB_ID=Vero4Chain
    CHAIN_ID=octopus
    if [ $TEST = false ];
	then
    	CHAIN_ID=octopus
	else
    	CHAIN_ID=squid
    fi;
    INITIAL_SUPPLY=1000000000000000000000000000000000000000000000000
fi;

case $(uname -m) in
    arm64) DOCKER_IMAGE=${DOCKER_ID}/tendermint-node-arm64:${VERSION};;
    aarch64) DOCKER_IMAGE=${DOCKER_ID}/tendermint-node-arm64:${VERSION};;
    x86_64) DOCKER_IMAGE=${DOCKER_ID}/tendermint-node:${VERSION};;
esac

echo "Starting the first node of a brand new $TYPE_CAPITALIZED blockchain version $VERSION:"
docker rm $TYPE 2>/dev/null >/dev/null
rm -r $DIR 2>/dev/null
mkdir -m700 $DIR

echo " * downloading the blockchain CLI"
mkdir $DIR/$CLI
cd $DIR/$CLI
wget --quiet https://github.com/${GITHUB_ID}/${TYPE}/releases/download/v${VERSION}/${CLI}_${VERSION}.tar.gz
tar zxf ${CLI}_${VERSION}.tar.gz
#cp -r ~/Gits/hotmoka/io-hotmoka-moka/modules .
#cp -r ~/Gits/hotmoka/io-hotmoka-moka/moka .
cd ../..

echo " * creating the key of the gamete"
read -s -p "     choose a password for the gamete: " PASSWORD
echo
cd $DIR
GAMETE_CREATION=$(./${CLI}/${CLI} create-key --password-of-new-key=${PASSWORD} --interactive=false)
cd ..
LINE2=$(echo "$GAMETE_CREATION"| sed '2!d')
GAMETE_PUBLIC_KEY_BASE58=${LINE2:19}

echo " * starting the docker container"
if [ $TEST = false ];
then
    docker run -dit --name ${TYPE} -e KEY_OF_GAMETE=${GAMETE_PUBLIC_KEY_BASE58} -e CHAIN_ID=${CHAIN_ID} -e INITIAL_SUPPLY=${INITIAL_SUPPLY} -p 80:8001 -p 26656:26656 -v chain:/home/${TYPE}/chain ${DOCKER_IMAGE} init >/dev/null
else
    docker run -dit --name ${TYPE} -e KEY_OF_GAMETE=${GAMETE_PUBLIC_KEY_BASE58} -e CHAIN_ID=${CHAIN_ID} -e INITIAL_SUPPLY=${INITIAL_SUPPLY} -e OPEN_UNSIGNED_FAUCET=true -p 80:8001 -p 26656:26656 -v chain:/home/${TYPE}/chain ${DOCKER_IMAGE} init >/dev/null
fi;
echo " * waiting for the node to complete its initialization"
sleep 10
echo "     waiting..."
while !(docker exec ${TYPE} bash -c "mkdir -p extract; cp -f chain/*.pem extract" 2>/dev/null)
do
    sleep 10
    echo "     waiting..."
done

echo " * extracting the pem of the validator account"
cd $DIR
docker cp ${TYPE}:/home/${TYPE}/extract/. . >/dev/null
VALIDATOR_ACCOUNT=$(ls *#?.pem)
VALIDATOR_ADDRESS=${VALIDATOR_ACCOUNT:0:66}
ln -s ${VALIDATOR_ACCOUNT} validator.pem
./${CLI}/${CLI} show-account ${VALIDATOR_ADDRESS} | tail -36 >${VALIDATOR_ADDRESS}_36_words.txt
cd ..

echo " * extracting the pem of the gamete account"
cd $DIR
GAMETE_BINDING=$(./${CLI}/${CLI} bind-key ${GAMETE_PUBLIC_KEY_BASE58} --uri ws://localhost:80)
LINE1=$(echo "$GAMETE_BINDING"| sed '1!d')
GAMETE_ADDRESS=${LINE1:14:66}
ln -s ${GAMETE_ADDRESS}.pem gamete.pem
./${CLI}/${CLI} show-account ${GAMETE_ADDRESS} | tail -36 >${GAMETE_ADDRESS}_36_words.txt
cd ..

# useful to fund it, so that it can later sell power
echo " * funding the validator account"
cd $DIR
./${CLI}/${CLI} send 500000 ${VALIDATOR_ADDRESS} --interactive=false --password-of-payer=${PASSWORD} --payer=${GAMETE_ADDRESS} --uri ws://localhost:80 >/dev/null
cd ..

if [ $TEST != false ];
then
    echo " * opening an unsigned faucet"
    cd $DIR
    ./${CLI}/${CLI} faucet 10000000000000 --interactive=false --password-of-gamete=${PASSWORD} --uri ws://localhost:80
    cd ..
fi;

echo " * cleaning up"
rm -r ${DIR}/${CLI}
rm $DIR/${GAMETE_PUBLIC_KEY_BASE58}.pem
PASSWORD=

echo
echo "The pem files to control the gamete and the validator have been saved in the directory \"${DIR}\"."
echo "Move that directory to the clients that need to control the node and delete it from this server."
echo
echo "The password of the gamete is what you have chosen above."
echo "The password of the validator is empty."
