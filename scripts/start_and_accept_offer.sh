#!/bin/bash

NETWORK_URL=${NETWORK_URL:=panarea.hotmoka.io}
echo "remote node = $NETWORK_URL"

# starts a validator node and creates a sale offer on behalf of validator #0
cp 67Zks6FvqBoyfmkfo6hpAaDa7AwYKPs3BDR2WWhigYWr.pem_ 67Zks6FvqBoyfmkfo6hpAaDa7AwYKPs3BDR2WWhigYWr.pem
echo "Starting Docker container"
CONTAINER_ID=$(docker run --rm -dit -e NETWORK_URL=$NETWORK_URL -p 80:8080 -p 26656:26656 -v chain:/home/hotmoka/chain hotmoka/tendermint-node:latest start)
echo $CONTAINER_ID
echo "Waiting for container to start up"
sleep 60
echo "Binding gamete"
../moka bind-key 67Zks6FvqBoyfmkfo6hpAaDa7AwYKPs3BDR2WWhigYWr --url localhost >/dev/null
INFO=$(../moka info --url localhost)
LINE=$(echo "$INFO"| grep "gamete:" | sed '1!d')
GAMETE=${LINE:14:66}
echo "Gamete = $GAMETE"
LINE=$(echo "$INFO"|grep "sale offer #0")
OFFER=${LINE: -66}
echo "Offer = $OFFER"
VALIDATOR1_KEY=$(docker exec $CONTAINER_ID /bin/ls|grep ".pem")
VALIDATOR1_KEY=${VALIDATOR1_KEY::-4}
echo "Validator #1 key = $VALIDATOR1_KEY"
echo "Recovering keys of validator #1"
docker cp $CONTAINER_ID:/home/hotmoka/$VALIDATOR1_KEY.pem .
echo "Creating validator #1"
RUN=$(../moka create-account 2000000000000 --create-tendermint-validator --payer $GAMETE --password-of-payer gamete-key --key-of-new-account $VALIDATOR1_KEY --url localhost --interactive=false)
LINE=$(echo "$RUN"|grep "A new account")
VALIDATOR1=${LINE:14:66}
echo "Validator #1 = $VALIDATOR1"
echo "Accepting offer of sale on behalf of validator #1"
../moka buy-validation $VALIDATOR1 $OFFER --interactive=false --password-of-buyer= --print-costs=false --url localhost
rm -f *.pem
