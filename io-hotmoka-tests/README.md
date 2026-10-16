<p align="center"><img width="320" src="../io-hotmoka-tutorial/src/main/resources/pics/hotmoka_logo.png" alt="Hotmoka logo"></p>

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

# Hotmoka tests

A collection of testcases on Takamaka smart contracts for the Hotmoka blockchain.

## Introduction

This subproject includes JUnit testcases of Takamaka smart contracts for the Hotmoka blockchain.
Such tests execute against the abstract blockchain interface of Hotmoka, by running
JUnit from inside Maven. It is possible to specify the configuration of the testing peer,
that acts as a local blockchain that executes the smart contracts. One can specify
the kind of peer (Mokamint for proof of space, Tendermint for proof of stake and stubbed
to test just the application layer of Hotmoka, at full speed), the number of peers in the
local blockchain, the block creation rate and the locality of the peer (local, directly invoked,
or remote, invoked through an internet connection in localhost).

Below, instructions are reported about the execution of these tests with various configurations.

## Prerequisites

You should have both Java (suggested 25 or later) and Maven installed in your machine to run the tests.
Moreover, if you want to run tests against a Tendermint blockchain,
the Tendermint executable must be installed in your machine as well.
You can download Tendermint from <a href="https://github.com/tendermint/tendermint/releases/tag/v0.34.15">https://github.com/tendermint/tendermint/releases/tag/v0.34.15</a>
(please download version 0.34.15, exactly). Put the Tendermint executable in a place
that is found in your command path, so that you can invoke it by simply typing `tendermint`.

Download the Hotmoka repository and compile it. Skip the tests for now:

```console
$ git clone git@github.com:Hotmoka/hotmoka.git
$ cd hotmoka
$ mvn clean install -DskipTests
```

This might take up to 20 minutes. At the end, enter in the subproject containing the tests on smart contracts:

```console
$ cd io-hotmoka-tests
```

## Running the tests

You can run the tests against a local blockchain with proof of stake consensus, built over Tendermint,
consisting of a single validator node that creates a block every four seconds (4000 milliseconds):

```console
$ mvn clean test -DnodeType=tendermint -DnumberOfNodes=1 -DtargetBlockCreationTime=4000
```

(This will take a long time: stop it with CTRL+C whenever you want).

You can run the same tests over another proof of stake
local blockchain, with a reduced the block creation rate
of three seconds and spawning four nodes, as follows:

```console
$ mvn clean test -DnodeType=tendermint -DnumberOfNodes=4 -DtargetBlockCreationTime=3000
```

You can run the same tests against a proof of space local blockchain consisting of a single
Mokamint node and creating a block every four seconds, on average:

```console
$ mvn clean test -DnodeType=mokamint -DnumberOfNodes=1 -DtargetBlockCreationTime=4000
```

You can run the same tests against a stubbed implementation that does not actually run a peer
and a block database but just executes the smart contracts with Hotmoka.
This is not a real blockchain but allows to run the tests much faster:

```console
$ mvn clean test -DnodeType=stubbed
```

You can run the same tests against a remote stubbed peer, connected through the internet in localhost rather
than called directly as a Java object:

```console
$ mvn clean test -DnodeType=stubbed-remote
```

You can do the same with `tendermint-remote` and `mokamint-remote`.

Some testcases depend on transactions being processed
by an exact time window (such as auctions and polls), which is problematic for blockchains such as
those based on Mokamint, whose proof-of-space consensus creates blocks at an *average* rate but where
each single block may have a specific creation time that deviates significantly from the average rate. 
Consequently, such testcases are programmed to greatly increase the time window if run on Mokamint, in order
to maximize the probability that transactions have been already committed by the time window.
As a consequence, timings with different blockchain implementations can be difficult to compare.
It is possible to get a more reliable comparison by disabling such testcases that work with a time window,
as in the following example:

```console
$ mvn clean test -DnodeType=mokamint -DnumberOfNodes=1 -DtargetBlockCreationTime=4000 -DonlyTimeIndependentTests
```

The information given above should be enough to imagine more alternative combinations
for the creation of the blockchain that executes the tests.

Note that some tests will likely fail on Mokamint blockchains with more than one node, since they
have probabilistic finality, which generates history changes that confuse the testcases.

## Generating a code coverage report

You can generate a code coverage report for an execution of the tests if you run the tests with the
`verify` target, from the parent module
(if you are inside `io-hotmoka-tests`, just `cd ..` before the following command). For instance:

```console
$ mvn clean verify -DnodeType=mokamint -DnumberOfNodes=1 -DtargetBlockCreationTime=4000 -DonlyTimeIndependentTests
```

The generated report will be created at `io-hotmoka-tests/target/site/jacoco-aggregate/index.html`.

&nbsp;

<p align="center"><img width="100" src="../io-hotmoka-tutorial/src/main/resources/pics/CC_license.png" alt="This documentation is licensed under a Creative Commons Attribution 4.0 International License"></p><p align="center">This document is licensed under a Creative Commons Attribution 4.0 International License.</p>

<p align="center">Copyright 2026 by Fausto Spoto (fausto.spoto@hotmoka.io).</p>