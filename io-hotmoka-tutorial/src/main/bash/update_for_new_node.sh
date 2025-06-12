#!/bin/bash

# This script updates the "replacements.sh" script
# so that it reflects the content of a remote node.
# It is useful after a new node has been deployed, if we want the
# documentation and the tutorial examples
# to reflect the actual content of the node.

java --module-path modules/explicit_or_automatic --class-path modules/unnamed --add-modules org.glassfish.tyrus.container.grizzly.server,org.glassfish.tyrus.container.grizzly.client --module io.hotmoka.tutorial/io.hotmoka.tutorial.UpdateForNewNode $NETWORK_URI
