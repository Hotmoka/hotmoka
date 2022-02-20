#!/bin/bash

# starts a validator node and creates a sale offer on behalf of validator #0
cp 67Zks6FvqBoyfmkfo6hpAaDa7AwYKPs3BDR2WWhigYWr.pem_ 67Zks6FvqBoyfmkfo6hpAaDa7AwYKPs3BDR2WWhigYWr.pem
echo "Starting Docker container"
docker run --rm -dit -e KEY_OF_GAMETE=67Zks6FvqBoyfmkfo6hpAaDa7AwYKPs3BDR2WWhigYWr -e CHAIN_ID=test -e INITIAL_SUPPLY=100000000000000000000000000 -p 80:8080 -p 26656:26656 -v chain:/home/hotmoka/chain hotmoka/tendermint-node:latest init
echo "Waiting for container to start up"
sleep 45
echo "Binding gamete"
moka bind-key 67Zks6FvqBoyfmkfo6hpAaDa7AwYKPs3BDR2WWhigYWr --url localhost >/dev/null
INFO=$(moka info --url localhost)
LINE=$(echo "$INFO"| grep "gamete:" | sed '1!d')
GAMETE=${LINE:14:66}
echo "Gamete = $GAMETE"
LINE=$(echo "$INFO"| grep "initial validator #0:" | sed '1!d')
VALIDATOR0=${LINE:31:67}
echo "Validator #0 = $VALIDATOR0"
echo "Recovering keys of validator #0"
sudo cp /var/lib/docker/volumes/chain/_data/$VALIDATOR0.pem .
echo "Charging validator #0"
moka send 1000000000000000 $VALIDATOR0 --payer=$GAMETE --password-of-payer=gamete-key --print-costs=false --interactive=false --url localhost
echo "Placing an offer of sale on behalf of validator #0"
SELL=$(moka sell-validation $VALIDATOR0 100000 3000 10000000 --password-of-seller= --interactive=false --print-costs=false --url localhost)
LINE1=$(echo "$SELL" | sed '1!d')
OFFER=${LINE1:6:66}
echo "Offer = $OFFER"
rm -f *.pem
