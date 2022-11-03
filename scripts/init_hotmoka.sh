#!/bin/bash

# an example of a scfript that starts a brand new Hotmoka blockchain
# initially consisting of a single node

DIR=hotmoka_node_info
VERSION=1.0.10

echo "Starting a brand new Hotmoka blockchain version $VERSION:"
docker rm hotmoka 2>/dev/null >/dev/null
rm -r $DIR 2>/dev/null
mkdir $DIR

echo " * downloading the blockchain CLI"
mkdir $DIR/moka
cd $DIR/moka
wget --quiet https://github.com/Hotmoka/hotmoka/releases/download/v${VERSION}/moka_${VERSION}.tar.gz
tar zxf moka_${VERSION}.tar.gz
cd ../..

echo " * creating key of gamete"
read -s -p "     choose a password for the gamete: " PASSWORD
echo
cd $DIR
GAMETE_CREATION=$(./moka/moka create-key --password-of-new-key=$PASSWORD --interactive=false)
cd ..
LINE2=$(echo "$GAMETE_CREATION"| sed '2!d')
GAMETE_PUBLIC_KEY_BASE58=${LINE2:19}

echo " * starting docker container"
docker run -dit --name hotmoka -e KEY_OF_GAMETE=$GAMETE_PUBLIC_KEY_BASE58 -e CHAIN_ID=marabunta -e INITIAL_SUPPLY=10000000000000000000000000000000000 -p 80:8080 -p 26656:26656 -v chain:/home/hotmoka/chain hotmoka/tendermint-node:latest init >/dev/null

echo " * waiting for the node to complete initialization"
sleep 10
echo "     waiting..."
while !(docker exec hotmoka bash -c "mkdir -p extract; cp -f chain/*.pem extract" 2>/dev/null)
do
    sleep 10
    echo "     waiting..."
done

echo " * extracting pem of the validator account"
cd $DIR
docker cp hotmoka:/home/hotmoka/extract/. .
VALIDATOR_ACCOUNT=$(ls *#?.pem)
VALIDATOR_ADDRESS=${VALIDATOR_ACCOUNT:0:66}
ln -s $VALIDATOR_ACCOUNT validator.pem
./moka/moka show-account $VALIDATOR_ADDRESS | tail -36 >${VALIDATOR_ADDRESS}_36_words.txt
cd ..

echo " * extracting pem of the gamete account"
cd $DIR
GAMETE_BINDING=$(./moka/moka bind-key $GAMETE_PUBLIC_KEY_BASE58 --url localhost:80)
LINE1=$(echo "$GAMETE_BINDING"| sed '1!d')
GAMETE_ADDRESS=${LINE1:14:66}
ln -s $GAMETE_ADDRESS.pem gamete.pem
./moka/moka show-account $GAMETE_ADDRESS | tail -36 >${GAMETE_ADDRESS}_36_words.txt
cd ..

echo " * cleaning up"
rm -r $DIR/moka
rm $DIR/$GAMETE_PUBLIC_KEY_BASE58.pem

echo
echo "The pem files to control the gamete and the validator are saved in the directory \"${DIR}\"."
echo "Move that directory to the clients that need to control the node and delete it from this server."
echo
echo "The password of the gamete is what you have chosen above."
echo "The password of the validator is empty."
