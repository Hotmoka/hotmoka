#!/bin/bash

MAX_GAS_PER_VIEW=${MAX_GAS_PER_VIEW:-1000000}

echo
echo "Restarting Hotmoka:"
echo "  MAX_GAS_PER_VIEW=$MAX_GAS_PER_VIEW"

moka restart-tendermint --tendermint-config=chain/blocks --max-gas-per-view ${MAX_GAS_PER_VIEW}
