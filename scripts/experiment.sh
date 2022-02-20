#!/bin/bash

echo "Binding gamete"
moka bind-key 7WRaVfVDr4f4bLcxPRGxXiYwkDZag2R4gFiLd9bKFtzF >/dev/null
echo "Opening faucet"
moka faucet 10000000000000000 --password-of-gamete=pippopippo --interactive=false
INFO=$(moka info)
LINE1=$(echo "$INFO"| grep "initial validator #0" | sed '1!d')
VALIDATOR0=${LINE1:31:67}
echo "Validator #0 = $VALIDATOR0"
echo "Recovering keys of validator #0"
sudo cp /var/lib/docker/volumes/chain/_data/$VALIDATOR0.pem .
sudo chown spoto.spoto $VALIDATOR0.pem
echo "Charging validator #0"
moka send 1000000000000000 $VALIDATOR0 --print-costs=false
echo "Creating validator #1"
CREATION=$(moka create-account 1000000000000000 --password-of-new-account= --create-tendermint-validator --interactive=false)
LINE2=$(echo "$CREATION" | sed '2!d')
VALIDATOR1=${LINE2:13:67}
echo "Validator #1 = $VALIDATOR1"
echo "Placing an offer of sale on behalf of validator #0"
SELL=$(moka sell-validation $VALIDATOR0 100000 3000 10000000 --password-of-seller= --interactive=false --print-costs=false)
LINE1=$(echo "$SELL" | sed '1!d')
OFFER=${LINE1:6:66}
echo "Offer = $OFFER"
echo "Buying the offer of validation power on behalf of validator #1"
moka buy-validation $VALIDATOR1 $OFFER --password-of-buyer= --print-costs=false --interactive=false
