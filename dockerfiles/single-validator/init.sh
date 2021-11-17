#!/bin/bash

INITIAL_SUPPLY=${INITIAL_SUPPLY:-10000000000000}
OPEN_UNSIGNED_FAUCET=${OPEN_UNSIGNED_FAUCET:-false}
KEY_OF_GAMETE=${KEY_OF_GAMETE:-missing}
CHAIN_ID=${CHAIN_ID:-missing}
MAX_GAS_PER_VIEW=${MAX_GAS_PER_VIEW:-1000000}
OBLIVION=${OBLIVION:-250000}
INITIAL_GAS_PRICE=${INITIAL_GAS_PRICE:-100}

echo
echo "Creating new Tendermint configuration:"
echo "  CHAIN_ID=$CHAIN_ID"

tendermint testnet --v 1 --o . >> /dev/null
# tendermint testnet creates a chain id of the form chain- followed
# by six characters: we replace it with the required chain id
sed -i "s/chain-....../$CHAIN_ID/g" node0/config/genesis.json
sed -i "s/create_empty_blocks = true/create_empty_blocks = false/g" node0/config/config.toml

echo
echo "Starting Hotmoka:"
echo "  INITIAL_SUPPLY=$INITIAL_SUPPLY"
echo "  OPEN_UNSIGNED_FAUCET=$OPEN_UNSIGNED_FAUCET"
echo "  KEY_OF_GAMETE=$KEY_OF_GAMETE"
echo "  MAX_GAS_PER_VIEW=$MAX_GAS_PER_VIEW"
echo "  OBLIVION=$OBLIVION"
echo "  INITIAL_GAS_PRICE=$INITIAL_GAS_PRICE"

# we delete the tendermint configuration that we have temporarily
# created into node0, so that we do not leave garbage around;
# in any case, it has been copied inside the chain directory
moka init-tendermint ${INITIAL_SUPPLY} --non-interactive --open-unsigned-faucet=${OPEN_UNSIGNED_FAUCET} --key-of-gamete=${KEY_OF_GAMETE} --takamaka-code /modules/explicit/io-takamaka-code-1.0.5.jar --tendermint-config=node0 --delete-tendermint-config --max-gas-per-view ${MAX_GAS_PER_VIEW} --oblivion ${OBLIVION} --initial-gas-price ${INITIAL_GAS_PRICE}
