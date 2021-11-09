#!/bin/bash

INITIAL_SUPPLY=${INITIAL_SUPPLY:-10000000000000}
OPEN_UNSIGNED_FAUCET=${OPEN_UNSIGNED_FAUCET:-false}
PASSWORD_OF_GAMETE=${PASSWORD_OF_GAMETE:-gamete}
CHAIN_ID=${CHAIN_ID:-marabunta}
MAX_GAS_PER_VIEW=${MAX_GAS_PER_VIEW:-1000000}

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
echo "  PASSWORD_OF_GAMETE=$PASSWORD_OF_GAMETE"
echo "  MAX_GAS_PER_VIEW=$MAX_GAS_PER_VIEW"

# we delete the tendermint configuration that we have temporarily
# created into node0, so that we do not leave garbage around;
# in any case, it has been copied inside the chain directory
moka init-tendermint ${INITIAL_SUPPLY} --non-interactive --open-unsigned-faucet=${OPEN_UNSIGNED_FAUCET} --password-of-gamete=${PASSWORD_OF_GAMETE} --takamaka-code /modules/explicit/io-takamaka-code-1.0.5.jar --tendermint-config=node0 --delete-tendermint-config --max-gas-per-view ${MAX_GAS_PER_VIEW}
