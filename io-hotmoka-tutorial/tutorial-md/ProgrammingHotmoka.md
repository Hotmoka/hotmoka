<p align="center"><img width="320" src="pics/hotmoka_logo.png" alt="Hotmoka logo"></p>

[![Java-Build Action Status](https://github.com/Hotmoka/hotmoka/actions/workflows/java_build.yml/badge.svg)](https://github.com/Hotmoka/hotmoka/actions)
[![Hotmoka@Maven Central](https://img.shields.io/maven-central/v/io.hotmoka/io-hotmoka-node.svg?label=Hotmoka@Maven%20Central)](https://central.sonatype.com/search?smo=true&q=g:io.hotmoka)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

Hotmoka is a framework for programming a network of communicating nodes, in a subset of Java called Takamaka. Nodes can belong to a blockchain or can be Internet of Things devices.

 The latest version of this document is available for free in PDF, ePub and MOBI format, at ![Hotmoka releases](https://github.com/Hotmoka/hotmoka/releases).

 <p align="center"><img width="100" src="pics/CC_license.png" alt="This documentation is licensed under a Creative Commons Attribution 4.0 International License"></p><p align="center">This document is licensed under a Creative Commons Attribution 4.0 International License.</p>

 <p align="center">Copyright 2022 by Fausto Spoto (fausto.spoto@hotmoka.io).</p>

 This software benefits from the use of the YourKit profiler for Java:

 ![Yourkit Logo](https://www.yourkit.com/images/yklogo.png)

 YourKit supports open source projects with innovative and intelligent tools for monitoring and profiling Java and .NET applications. YourKit is the creator of <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>, <a href="https://www.yourkit.com/.net/profiler/">YourKit .NET Profiler</a>, and <a href="https://www.yourkit.com/youmonitor/">YourKit YouMonitor</a>.

# Table of Contents
1. [Introduction](#introduction)
2. [Getting Started with Hotmoka](#getting-started-with-hotmoka)
    - [Hotmoka in a Nutshell](#hotmoka-in-a-nutshell)
    - [Hotmoka Clients](#hotmoka-clients)
        - [Moka](#moka)
        - [Mokito](#mokito)
    - [Contacting a Hotmoka Test Node](#contacting-a-hotmoka-test-node)
    - [Creation of a First Account](#creation-of-a-first-account)
    - [Importing Accounts](#importing-accounts)
    - [Anonymous Payments](#anonymous-payments)
    - [Installation of the Source Code](#installation-of-the-source-code)
3. [A First Takamaka Program](#a-first-takamaka-program)
    - [Creation of the Eclipse Project](#creation-of-the-eclipse-project)
    - [Installation of the Jar in a Hotmoka Node](#installation-of-the-jar-in-a-hotmoka-node)
    - [Creation of an Object of our Program](#creation-of-an-object-of-our-program)
    - [Calling a Method on an Object in a Hotmoka Node](#calling-a-method-on-an-object-in-a-hotmoka-node)
    - [Storage Types and Constraints on Storage Classes](#storage-types-and-constraints-on-storage-classes)
    - [Transactions Can Be Added, Posted and Run](#transactions-can-be-added-posted-and-run)
4. [The Notion of Smart Contract](#the-notion-of-smart-contract)
    - [A Simple Ponzi Scheme Contract](#a-simple-ponzi-scheme-contract)
    - [The `@FromContract` and `@Payable` Annotations](#the-fromcontract-and-payable-annotations)
    - [Payable Contracts](#payable-contracts)
    - [The `@View` Annotation](#the-view-annotation)
    - [The Hierarchy of Contracts](#the-hierarchy-of-contracts)
5. [The Support Library](#the-support-library)
    - [Storage Lists](#storage-lists)
        - [A Gradual Ponzi Contract](#a-gradual-ponzi-contract)
        - [A Note on Re-entrancy](#a-note-on-re-entrancy)
        - [Running the Gradual Ponzi Contract](#running-the-gradual-ponzi-contract)
    - [Storage Arrays](#storage-arrays)
        - [A Tic-Tac-Toe Contract](#a-tic-tac-toe-contract)
        - [A More Realistic Tic-Tac-Toe Contract](#a-more-realistic-tic-tac-toe-contract)
        - [Running the Tic-Tac-Toe Contract](#running-the-tic-tac-toe-contract)
        - [Specialized Storage Array Classes](#specialized-storage-array-classes)
    - [Storage Maps](#storage-maps)
        - [A Blind Auction Contract](#a-blind-auction-contract)
        - [Events](#events)
        - [Running the Blind Auction Contract](#running-the-blind-auction-contract)
        - [Listening to Events](#listening-to-events)
6. [Tokens](#tokens)
    - [Fungible Tokens (ERC20)](#fungible-tokens-erc20)
        - [Implementing Our Own ERC20 Token](#implementing-our-own-erc20-token)
    - [Richer than Expected](#richer-than-expected)
    - [Non-Fungible Tokens (ERC721)](#non-fungible-tokens-erc721)
        - [Implementing Our Own ERC721 Token](#implementing-our-own-erc721-token)
7. [Hotmoka Nodes](#hotmoka-nodes)
    - [Tendermint Nodes](#tendermint-nodes)
    - [Disk Nodes](#disk-nodes)
    - [Logs](#logs)
    - [Node Decorators](#node-decorators)
    - [Hotmoka Services](#hotmoka-services)
    - [Remote Nodes](#remote-nodes)
        - [Creating Sentry Nodes](#creating-sentry-nodes)
    - [Signatures and Quantum-Resistance](#signatures-and-quantum-resistance)
8. [Tendermint Hotmoka Nodes](#tendermint-hotmoka-nodes)
    - [Starting a Tendermint Hotmoka Node with Docker](#starting-a-tendermint-hotmoka-node-with-docker)
    - [Manifest and Validators](#manifest-and-validators)
    - [Starting a Tendermint Hotmoka Node on Amazon EC2](#starting-a-tendermint-hotmoka-node-on-amazon-ec2)
    - [Connecting a Tendermint Hotmoka Node to an Existing Blockchain](#connecting-a-tendermint-hotmoka-node-to-an-existing-blockchain)
    - [Shared Entities](#shared-entities)
    - [Becoming a Validator](#becoming-a-validator)
9. [Code Verification](#code-verification)
    - [JVM Bytecode Verification](#jvm-bytecode-verification)
    - [Takamaka Bytecode Verification](#takamaka-bytecode-verification)
    - [Command-Line Verification and Instrumentation](#command-line-verification-and-instrumentation)
10. [References](#references)

# Introduction

More than a decade ago, Bitcoin [[Nakamoto08]](#references)
swept the computer industry
as a revolution, providing, for the first time, a reliable technology
for building trust over an inherently untrusted computing
infrastructure, such as a distributed network of computers.
Trust immediately translated into money and Bitcoin became
an investment target, exactly at the moment of one of the worst
economical turmoil of recent times. Central(-_ized_) banks,
fighting against the crisis, looked like dinosaurs in comparison
to the _decentralized_ nature of Bitcoin.

Nevertheless, the novelty of Bitcoin was mainly related to its
_consensus_ mechanism based on a _proof of work_, while the
programmability of Bitcoin transactions was limited due
to the use of a non-Turing-equivalent scripting
bytecode [[Antonopoulos17]](#references).

The next step was hence the use of a Turing-equivalent
programming language (up to _gas limits_) over an abstract
store of key/value pairs, that can be
efficiently kept in a Merkle-Patricia trie.
That was Ethereum [[AntonopoulosW19]](#references), whose
Solidity programming language allows one
to code any form of _smart contract_, that is, code
that becomes an agreement between parties, thanks to
the underlying consensus enforced by the blockchain.

Solidity looks familiar to most programmers. Conditionals, loops and
structures are there since more than half a century. Programmers
assumed that they _knew_ Solidity. However, the intricacies of
its semantics made learning Solidity harder than expected.
Finding good Solidity programmers is still difficult and
they are consequently expensive. It is, instead, way too easy
to write buggy code in Solidity, that _seems_ to work perfectly,
up to _that_ day when things go wrong, very wrong [[AtzeiBC17]](#references).

It is ungenerous to blame Solidity for all recent attacks to smart contracts
in blockchain. That mainly happened because of the same success of Solidity,
that made it the natural target of the attacks. Moreover, once the
Pandora's box of Turing equivalence has been opened, you cannot expect anymore to
keep the devils at bay, that is, to be able to
decide and understand, exactly, what your code will do at run time.
And this holds for every programming language, past, present or future.

I must confess that my first encounter with Solidity
was a source of frustration. Why was I expected to learn another programming
language? and another development environment? and another testing framework?
Why was I expected to write code without a support library that provides
proved solutions to frequent problems?
What was so special with Solidity after all? Things became even more difficult when
I tried to understand the semantics of the language. After twenty-five years of studying
and teaching programming languages, compilation, semantics and code analysis
(or, possibly, just because of that) I still cannot explain exactly why there
are structures and contracts instead of a single composition mechanism in Solidity;
nor what is indeed the meaning of `memory` and `storage` and why
it is not the compiler that takes care of such gritty details; nor why
externally owned accounts are not just a special kind of contracts;
nor why Solidity needs such low-level (and uncontrollable)
call instructions, that make Java's (horrible) reflection, in comparison, look like
a monument to clarity;
nor why types are weak in Solidity, so that contracts are held in `address`
variables, whose actual type is unknown and cannot be easily
enforced at run time [[CrafaPZ19]](#references), with all consequent
programming monsters, such as unchecked casts. It seems that the evolution
of programming languages has brought us back to C's `void*` type.

Hence, when I first met people from Ailia SA in fall 2018, I was not surprised
to realize that they were looking for a new way of programming smart contracts
over the new blockchain that they were developing. I must thank them and our useful
discussions, that pushed me to dive in blockchain technology and
study many programming languages for smart contracts. The result
is Takamaka, a Java framework for writing smart contracts.
This means that it allows programmers to use a subset of Java for writing code
that can be installed and run in blockchain. Programmers will not have
to deal with the storage of objects in blockchain: this is completely
transparent to them. This makes Takamaka completely different from other
attempts at using Java for writing smart contracts, where programmers
must use explicit method calls to persist data to blockchain.

Writing smart contracts in Java entails that programmers
do not have to learn yet another programming language.
Moreover, they can use a well-understood and stable development
platform, together with all its modern tools. Programmers can use
features from the latest versions of Java, including lambda
expressions.
There are, of course, limitations to the kind of code that can
be run inside a blockchain. The most important limitation is
that programmers can only call a portion of the huge Java library,
whose behavior is deterministic, whose cost is predictable and whose methods are guaranteed
to terminate.

The runtime of the Takamaka programming language
is included in the Hotmoka project, a framework
for collaborating nodes, whose long-term goal is to unify the programming
model of blockchain and internet of things.
The more scientific aspects of Hotmoka and Takamaka have been published
in the last years [[BeniniGMS21]](#references)[[CrosaraOST21]](#references)[[OlivieriST21]](#references)[[Spoto19]](#references)[[Spoto20]](#references).

**Intended Audience**.
This book is for software developers who want to use Hotmoka nodes and program smart contracts in Takamaka.
It goes deep into the inner working of Hotmoka. For instance, it shows how transactions can be
triggered in code, not just with the Moka client of Hotmoka. Less experienced readers, or
developers not interested in writing code that interacts with Hotmoka nodes, can just skip these
parts and concentrate on the use of the Moka command-line client only. Non-technical users might
just be happy with the use of Mokito, the mobile and web clients of Hotmoka, whose
functionalities are limited of course.

**Contribute to Hotmoka**.
Hotmoka is a complex project, that requires many and different skills. After years of development,
it is ready for the general public. This does not mean that it is bug-free, nor perfect:
we expect our users to find all sort of bugs and to suggest improvements. Hence, feel
free to write to us at `fausto.spoto@hotmoka.io`, with bugs and improvement requests.
If you are a developer, consider the possibility of helping us with the development
of the project. In particular, the whole ecosystem of applications running
over Hotmoka is missing at the moment (that is, applications, typically web-based, that
use Hotmoka as their backend storage). Hotmoka is open-source and non-proprietary,
licensed under the terms of the Apache 2.0 License. Therefore, feel free to clone and fork the code.

**The Example Projects of This Book**.
The experiments that we will perform in this book will
require one to create Java projects. We suggest that you create and
experiment with these projects yourself.
However, if you have no time and want to jump immediately to the result,
or if you want to compare your work
with the expected result, we provide you with the completed examples of this book in
a module of the Hotmoka distribution repository, that you can clone.
Each section of this book will report
the project of the repository where you can find the related code.
You can clone the code as follows:

```shell
$ git clone --branch v1.9.0 https://github.com/Hotmoka/hotmoka.git
```

You will find the examples of Takamaka smart contracts inside the Maven module
`io-hotmoka-tutorial-examples`.

_Verona, June 2025_.

&nbsp;

**Acknowledgments**.
I thank the people at Ailia SA, in particular Giovanni Antino, Mario Carlini,
Iris Dimni and Francesco Pasetto, who decided to invest in the Takamaka project and who are building their own
open-source blockchain that can be programmed in Takamaka. My thank goes also to all students and
colleagues who have read and proof-checked this book and its examples, finding
bugs and inconsistencies; in particular to
Luca Olivieri and Fabio Tagliaferro.
Chapter [Tokens](#tokens) is a shared work with Marco Crosara, Filippo Fantinato, Luca Olivieri and Fabio Tagliaferro.
Chapter [Hotmoka Nodes](#hotmoka-nodes) has been inspired by previous work with Dinu Berinde.
Section [Shared Entities](#shared-entities) is a shared work
with Andrea Benini, Mauro Gambini and Sara Migliorini.

&nbsp;

 <p align="center"><img width="200" src="pics/docker-hub.png" alt="DockerHub logo"></p><p>Hotmoka enjoys being a <a href="https://docs.docker.com/trusted-content/dsos-program/">Docker-sponsored open source program</a>. <a href="https://hub.docker.com/">DockerHub</a> provides for free a repository for the distribution of the Docker images of Hotmoka.</p>


&nbsp;

 <p align="center"><img width="200" src="pics/github.png" alt="GitHub logo"></p><p><a href="https://github.com/">GitHub</a> is hosting the code of Hotmoka for free, running tests and packaging actions at each commit and hosting its releases for download.</p>


&nbsp;

 <p align="center"><img width="200" src="pics/YourKit.png" alt="YourKit logo"></p><p>Hotmoka benefits from the use of a free license of the YourKit profiler for Java. YourKit supports open source projects with innovative and intelligent tools for monitoring and profiling Java and .NET applications. YourKit is the creator of <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>, <a href="https://www.yourkit.com/.net/profiler/">YourKit .NET Profiler</a>, and <a href="https://www.yourkit.com/youmonitor/">YourKit YouMonitor</a>.</p>


# Getting Started with Hotmoka

## Hotmoka in a Nutshell

Hotmoka is the abstract definition of a device that can store
objects (data structures) in its persistent memory (its _state_)
and can execute, on those objects,
code written in a subset of Java called Takamaka. Such a device is
called a _Hotmoka node_ and such programs are known as
_smart contracts_, taking that terminology from programs that run inside
a blockchain. It is well true that Hotmoka nodes can be different from the nodes
of a blockchain (for instance, they can be an Internet of Things device);
however, the most prominent application of Hotmoka nodes is, at the
moment, the construction of blockchains whose nodes are Hotmoka nodes.

Every Hotmoka node has its own persistent state, that contains code and
objects. Since Hotmoka nodes are made for running Java code, the code
inside their state is kept in the standard jar format used by Java, while objects
are just a collection of values for their fields, with a class tag that identifies
whose class they belong to and a reference (the _classpath_)
to the jar where that class is defined.
While a device of an Internet of Thing network is the sole responsible
for its own state, things are different if a Hotmoka node that is part of a blockchain.
There, the state is synchronized and identical across all nodes of the blockchain.

In object-oriented programming, the units of code that can be run
on an object are called _methods_.
When a method must be run on an object,
that object is identified as the _receiver_ of the execution of the method.
The same happens in Hotmoka. That is, when one wants to run a method
on an object, that object must have been already allocated
in the state of the node and must be marked as the receiver of the execution
of the method. Assume for instance that one wants to run a method
on the object in Figure 1, identified as receiver.
The code of the method is contained in a jar, previously installed in the state
of the node, and referred as _classpath_. This is the jar where the class of
the receiver is defined.

 <p align="center"><img width="400" src="pics/receiver_payer.png" alt="Figure 1. Receiver, payer and classpath for a method call in a Hotmoka node"></p><p align="center">Figure 1. Receiver, payer and classpath for a method call in a Hotmoka node.</p>


The main difference with standard object-oriented programming is that Hotmoka requires one
to specify a further object, called _payer_. This is because a Hotmoka node is
a public service, that can be used by everyone has an internet connection
that can reach the node. Therefore, that service must be paid with the
internal cryptocurrency of the node, by providing a measure of execution
effort known as _gas_. The payer is therefore a sort of bank account, whose
balance gets decreased in order to pay for the gas needed for the execution of the method.
The payer is accessible inside the method as its _caller_.

> There are many similarities with what happens in Ethereum: the notion of
> receiver, payer and gas are taken from there. There are, however, also
> big differences. The first is that the code of the methods is inside
> a jar _referenced_ by the objects, while Ethereum requires to reinstall
> the code of the contracts each time a contract is instantiated.
> More importantly, Hotmoka keeps an explicit class tag inside the objects,
> while contracts are untyped in Ethereum [[CrafaPZ19]](#references)
> and are referenced through the untyped `address` type.

Receiver and payer have different roles but are treated identically in Hotmoka:
they are objects stored in state at their respective state locations, known as
their _storage references_. For instance the payer in
Figure 1 might be allocated at the storage
reference `cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0`. A storage reference has two parts, separated
by a `#` sign. The first part are 64 hexadecimal digits (ie, 32 bytes)
that identify the
transaction that created the object; the second part is a progressive number
that identifies an object created during that transaction: the first object
created during the transaction has progressive zero, the second has progressive
one, and so on. When a method is called on a Hotmoka node, what is actually specified
in the call request are the storage references of the receiver and of the payer
(plus the actual arguments to the method, if any).

In Hotmoka, a _transaction_ is either

1. the installation of a jar, that modifies the state of the node, and is paid by a payer account, or
2. the execution of a constructor, that yields the storage reference of a new object, or
3. the execution of a method on a receiver, that yields a returned
   value and/or has side-effects that modify the state of the node, and is paid by a payer account.

A Hotmoka node can keep track
of the transactions that it has executed, so that it is possible, for instance,
to recreate its state by running all the transactions executed in the past, starting from
the empty state.

It is very important to discuss at this moment a significant difference with what
happens in Bitcoin, Ethereum and most other blockchains. There, an account
is not an object, nor a contract,
but just a key in the key/value store of the blockchain, whose value is its balance.
The key used for an account is typically computed by hashing the public key derived from
the private key of the account. In some sense, accounts, in those blockchains, exist
independently from the state of the blockchain and can be computed offline: just
create a random private key, compute the associated public key and hence its hash.
Hotmoka is radically different: an account is an object that must be allocated in
state by an explicit construction transaction (that must be paid, as every transaction).
The public key is explicitly stored inside the constructed object
(Base64-encoded in its `publicKey` field, see Figure 1).
That public key was passed as a parameter at the creation of the payer object and
can be passed again for creating more accounts. That is, it is well possible, in Hotmoka,
to have more accounts in the state of a node, all distinct, but controlled by the same key.
It is also possible to replace the key of an account with another key, since the
`publicKey` field is not `final`.

This has a major consequence. In Bitcoin and Ethereum, an account is identified by
twelve words and a password, by using the BIP39 encoding
(see Figure 5-6 of [[Antonopoulos17]](#references)). These twelve words are just
a mnemonic representation of 132 bits: 128 bits for the random entropy used to derive the
private key of the account and four bits of checksum. In Hotmoka, these 128 bits are
not enough, since they identify the key of the account but not the 32 bytes of its
storage reference (in this representation, the progressive is assumed to be zero).
As a consequence, accounts in Hotmoka are identified by
128+256 bits, plus 12 bits of checksum (and a password), which give rise to 36 words in BIP39 encoding.
By specifying those 36 words across different clients, one can control the
same account with all such clients. As usual, those 36 words must be stored in paper
and kept in a secure place, since their lost amounts to losing access to the account.

As shown in Figure 1, the code of the objects (contracts) installed in
the state of a Hotmoka node consists in jars (Java archives) written in a subset of Java
known as Takamaka. This is done in a way completely different from other blockchains:

1. In Hotmoka, programmers code the contracts that want to install in the node and nothing more;
   they do _not_ program the encoding of data into the key/value store of the node (its _keeper_,
   as it is called in other blockchains); they do _not_ program the gas metering; they do _not_
   program the authentication of the accounts and the verification of their credentials.
   Everything is automatic in Hotmoka, exactly as in Ethereum, and differently from other blockchains
   that use general purpose languages such as Java for their smart contracts: there, programmers
   must take care of all these details, which is difficult, boring and error-prone. If this is done
   incorrectly, those blockchains will hang.
2. In Hotmoka, the code installed in the node passes a preliminary verification, that checks the correct
   use of some primitives, that we will introduce in the subsequent chapters, and guarantees that the
   code is deterministic. This excludes an array of errors in Hotmoka, while other blockchains
   will hang if, for instance, the code is non-deterministic.

## Hotmoka Clients

In order to query a Hotmoka node, handle accounts and run transactions on the node,
one needs a client application. Currently, there are a command-line client, called
Moka and a mobile client for Android, called Mokito. Mokito provides
basic functionalities only (handling accounts, querying the state of the objects in the node,
running simple transactions), while Moka is the most complete solution.

### Moka

You can use the `moka` command to interact with a Hotmoka node,
install code in the node and run transactions. There are two ways of using `moka`.
You can either download its source code, compile it and add the `moka` executable to
the command path of your machine; or you can use `moka` inside its Docker container.
The former approach is more flexible but requires to have
Java JDK version 21 (or higher) installed in your
computer, along a recent version of Maven. The latter approach avoids to install
and compile software on your machine, but you need to have Docker installed of course.

#### Downloading and compiling `moka`

If you want to install `moka` under `~/Opt`, under Linux or MacOS you can run the following commands:

````shell
$ cd ~/Opt
$ git clone --branch v1.9.0 https://github.com/Hotmoka/hotmoka.git
$ cd hotmoka
$ mvn clean install -DskipTests
$ export PATH=$PATH:$(pwd)/io-hotmoka-moka
````

> The dollar sign is the prompt of the shell.

The last `export` command expands the command path of the shell with
the `~/Opt/hotmoka/io-hotmoka-moka` directory, so that `moka` can
be invoked from the command shell, regardless of the current directory.
You might want to add an `export`
at the end of your `~/.bashrc` configuration file, so that the command path
will be expanded correctly the next time you open a shell. For instance, I added
the following command at the end of my `~/.bashrc`:

```shell
$ export PATH=/home/spoto/Opt/hotmoka/io-hotmoka-moka:$PATH
```

You should now be able to invoke `moka`:

```shell
$ moka --version
1.9.0
```

The process is similar under Windows: you will add
the directory containing `moka` to the `PATH` environment variable.

#### Invoking `moka` from inside its docker container

There are a few docker containers embedding `moka` inside of them.
For instance you can call `moka` as follows:

```shell
$ docker run -it hotmoka/mokamint-node:1.9.0 moka --version
1.9.0
```

This time you do not need Java nor Maven, nor to compile anything: docker will take care
of downloading the image of the container and run `moka` inside it.

#### First Usage of `moka`

In the following examples, we show direct invocations of `moka`, without the docker container.
Remember, however, that you can also run it from inside its docker container if you prefer.

You can check the options of `moka` as follows:

```shell
$ moka help
This is the command-line interface of Hotmoka.
Usage: moka [--version] [COMMAND]
      --version   print version information and exit
Commands:
  help      Display help information about the specified command.
  accounts  Manage Hotmoka accounts.
  jars      Manage jars of Takamaka classes.
  keys      Manage cryptographic key pairs.
  nodes     Manage Hotmoka nodes.
  objects   Manage Hotmoka objects.
Copyright (c) 2021 Fausto Spoto (fausto.spoto@hotmoka.io)
```

As you can see above, the `moka help` command prints a description
of the available subcommands and exits.
You can have a detailed help of a specific subcommand
by specifying the subcommand after `help`.
For instance, to print the help of the
`objects` subcommand, you can type:

```shell
$ moka help objects
Manage Hotmoka objects.
Usage: moka objects [COMMAND]
Commands:
  help    Display help information about the specified command.
  call    Call a method of an object or class.
  create  Create a storage object.
  show    Show the state of a storage object.
```

You can print the help of a specific leaf command by prefixing `help` before it:

```shell
$ moka objects help show
Show the state of a storage object.
Usage: moka objects show [--api] [--json] [--timeout=<milliseconds>]
                         [--uri=<uri>] <object>
      <object>      the object
      --api         print the public API of the object
      --json        print the output in JSON
      --timeout=<milliseconds>
                    the timeout of the connection
                      Default: 20000
      --uri=<uri>   the network URI where the API of the Hotmoka node service
                      is published
                      Default: ws://localhost:8001
```

### Mokito

The `moka` tool allows one to perform a large variety of operations
on a Hotmoka node. However, it is a technical tool, meant for developers.
Most users will only perform simple tasks with a Hotmoka node.
For them, it is simpler to use a mobile app, with a simpler user
interface. That app, called `Mokito`, is currently available for Android only.
You can download it from Google Play and install it in your device, from
[https://play.google.com/store/apps/details?id=io.hotmoka.android.mokito](https://play.google.com/store/apps/details?id=io.hotmoka.android.mokito). Developers interested in its Kotlin source code
can find it at
[mokito_repo](https://github.com/Hotmoka/HotmokaAndroid),
together with a small Android service for connecting to a remote Hotmoka node.

The first time you will use Mokito on your mobile device,
it will connect by default to our testnet Hotmoka node
and show the screen in Figure 2. This can be changed
in the preferences section of the app, accessible through the menu in the
top left area of the app.

 <p align="center"><img width="300" src="pics/mokito_start.png" alt="Figure 2. The starting screen of the Mokito app"></p><p align="center">Figure 2. The starting screen of the Mokito app.</p>


## Contacting a Hotmoka Test Node

The examples in this book must be run against a Hotmoka node,
typically part of a Hotmoka blockchain. We will show you in a later chapter how you
can install your own local
node or blockchain. However, for now, it is much simpler to experiment with a node
that is part of one of the public
test blockchains that we provide for experimentation.
Namely, we have installed two Hotmoka nodes for testing, of two distinct blockchains, at URIs
`ws://panarea.hotmoka.io:8001` and `ws://panarea.hotmoka.io:8002`.
The peculiarity of these nodes is that they include a _faucet_ that gives
away small amounts of coins, when requested. This is good for experimentation
but, of course, a real node will not include a faucet.
In a real node, people must grasp some coins because they have been earned through mining,
sent by some other user or bought from some exchange.

You can verify that you can contact the test node by typing
the command `moka nodes manifest show` to print the _manifest_ information
about a Hotmoka node at an address, as you can see below:

```shell
$ moka nodes manifest show --uri ws://panarea.hotmoka.io:8001
  takamakaCode: 48a393014e839351b1bf56bdf9f127601f49b938d27e2c890fc8dd3e08099182
  manifest: 4d5bb808bb15b47ff2eabd0e8c187da99477eab2dfe4f720dafc0035eaacf444#0
    chainId: octopus
    maxErrorLength: 300
    signature: ed25519
    ...
    gamete: 232f281dbf1477cf843eff176083808087ee9ef4ec7543607637b2aaeb0534e9#0
      balance: 99999999999999999999...
      maxFaucet: 10000000000000000
      ...
    gasStation: 4d5bb808bb15b47ff2eabd0e8c187da99477eab2dfe4f720dafc0035eaacf444#f
      gasPrice: 1
      ...
    validators: 4d5bb808bb15b47ff2eabd0e8c187da99477eab2dfe4f720dafc0035eaacf444#1
      currentSupply: 1000000000000000...
      ...
```
The details of this information are irrelevant for now, but something must be
clarified, to understand the following sections better.
Namely, the `moka nodes manifest show` command reports information that tells us that the node
already contains some code and some Java objects, as shown in Figure 4.

 <p align="center"><img width="650" src="pics/state1.png" alt="Figure 4. The state of the test network nodes"></p><p align="center">Figure 4. The state of the test network nodes.</p>


The `takamakaCode` reference is the pointer to a jar, installed in blockchain, that contains
the classes of the Takamaka language runtime. All programs that we will write in Takamaka
will depend on that jar, since they will use classes such
as `io.takamaka.code.lang.Contract` or annotations such as `io.takamaka.code.lang.View`.
The `manifest` reference, instead, points to a Java object that publishes
information about the node. Namely, it reports its chain identifier and
the signature algorithm that, by default, is used to sign the transactions for the node. The manifest points
to another object, called `gamete`, that is an account holding all coins
in blockchain, initially. Consequently, the gamete has a rich `balance`,
but also another interesting property, called `maxFaucet`, that states how much coins
it is willing to give up for free. In a real node, and differently from here,
that value should be zero. In this test network, instead, it is a non-zero value
that we will exploit for creating our first account, in
a minute. The `gasStation` refers to another Java object, that provides
information about the gas, such as its current `gasPrice`. Finally, there is
another Java object, called `validators`, that keeps information about the validator nodes of the network
(if the network has validators) and contains, as well, the initial, current and final supply of cryptocurrency
(how much cryptocurrency was minted at the beginning of the network, has much has been minted up to now,
and how much will exist eventually).

As we said in the previous section, Java objects in the Hotmoka node are identified by their
_storage reference_, such as `4d5bb808bb15b47ff2eabd0e8c187da99477eab2dfe4f720dafc0035eaacf444#f`.
You can think at a storage reference as a machine-independent pointer inside the
memory, or state, of the node.

We have used the `moka` tool to see the manifest of a node. You can also use the
Mokito app for that. Namely, tap on the app menu icon on the top-left corner of the screen
and select _Manifest_ from the menu that will appear (see Figure 5).

 <p align="center"><img width="300" src="pics/mokito_menu.png" alt="Figure 5. The menu of the Mokito app"></p><p align="center">Figure 5. The menu of the Mokito app.</p>


After tapping on _Manifest_, a new screen will appear, containing the same information
that we saw with `moka nodes manifest show` (see Figure 6).

 <p align="center"><img width="300" src="pics/mokito_manifest.png" alt="Figure 6. The manifest of the Hotmoka node, shown in the Mokito app"></p><p align="center">Figure 6. The manifest of the Hotmoka node, shown in the Mokito app.</p>


## Creation of a First Account

We need an account in the test network, that we will use later to pay for
installing code in blockchain and for running transactions. An account in Hotmoka
is something completely different from an account in other blockchains.
For instance, in Bitcoin and Ethereum, accounts do not really exist, in the sense
that they are just an address derived from a private key, that can be used to control
information in blockchain. Their creation does not trigger any operation in blockchain:
it is performed completely off-chain. Instead, in Hotmoka, an account
is a Java object, more precisely an instance of the `io.takamaka.code.lang.ExternallyOwnedAccount`
class. That object must be allocated (_created_) in the memory of the Hotmoka node,
before it can be used. Moreover, such an object is not special in any way:
for instance, as for all other objects in the storage of the node, we must pay for its creation.
Currently, we have no accounts and consequently
no coins for paying the creation of a new object. We could
earn coins by mining for the network, or as payment for some activity,
or by buying coins at an exchange.
Since this is a test network, we can more simply use the faucet of the gamete instead, that is willing
to send us up to 10000000000000000 coins, for free. Namely, you can run the
following commands in order to create a key pair and then ask the faucet to create your first externally owned account
for that key pair,
funded with 50000000000000 coins, initially, paid by the faucet. Execute the following commands
inside a `hotmoka_tutorial` directory of your home, so that `moka` will save the key pair of your account
there, which will simplify your subsequent work:

```shell
$ moka keys create --name=account1.pem --password
Enter value for --password
  (the password that will be needed later to use the key pair): chocolate
The new key pair has been written into "account1.pem":
* public key: FqGhFHBkePzSqpgR9XwkxHtS9sUbLnRRoQb6kYebMwZZ (ed25519, base58)
* public key: 3GE9ryOG4bukXpam8RWJM40NGJQKInfNDL3a7BIq3pg= (ed25519, base64)
* Tendermint-like address: 342131C32CE14A5AC3FD93B1786AD28453901A3F

$ moka accounts create faucet 50000000000000 account1.pem --password
    --uri ws://panarea.hotmoka.io:8001
Enter value for --password
  (the password of the key pair): chocolate
Adding transaction cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216... done.
A new account cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0 has been created.
Its key pair has been saved
  into the file "cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0.pem".

Gas consumption:
 * total: 6720
   * for CPU: 2445
   * for RAM: 3816
   * for storage: 459
   * for penalty: 0
 * price per unit: 1 pana
 * total price: 6720 panas
```

An object has been created in the node, identified by its
*storage reference*, that in this case is `cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0`.

> Note that this reference will be different in your machine, as well as the 36 words passphrase.
> Change these accordingly in the subsequent examples.

This storage reference is a machine-independent pointer to your account Java object, inside
the node. Moreover, a random sequence of bits, called _entropy_, has been generated
and saved into a `.pem` file. From that entropy, and the chosen password, it is possible
to derive private and (hence) public key of the account. This is why we use
_key pair_ to refer to such `.pem.` file.
You should keep the `.pem` file secret since, together with the
password of the account (in our case, we chose `chocolate`),
it allows its owner to control your account and spend its coins.
Note that the password is not written anywhere: if you lose it, there is
no way to recover that password.

Let us check that our account really exists at its address,
by querying the node about the state of the object allocated at `cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0`:

```shell
$ moka objects show cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    --uri ws://panarea.hotmoka.io:8001
class io.takamaka.code.lang.ExternallyOwnedAccountED25519
  (from jar installed at 48a393014e839351b1bf56bdf9f127601f49b938d27e2c890fc8dd3e08099182)
  io.takamaka.code.lang.Contract.balance:java.math.BigInteger = 50000000000000
  io.takamaka.code.lang.ExternallyOwnedAccount.nonce:java.math.BigInteger = 0
  io.takamaka.code.lang.ExternallyOwnedAccount.publicKey:java.lang.String
    = "3GE9ryOG4bukXpam8RWJM40NGJQKInfNDL3a7BIq3pg="
```

Note that the balance and the public key of the account are
fields of the account object. Moreover, note that Hotmoka knows
which is the class of the object at that address
(it is a `io.takamaka.code.lang.ExternallyOwnedAccount`)
and where that class is defined (inside the jar
at address `48a393014e839351b1bf56bdf9f127601f49b938d27e2c890fc8dd3e08099182`, that is, `takamakaCode`).

> This is completely different from what happens, for instance,
> in Ethereum, where externally owned accounts and contract references and untyped at run time,
> so that it is not possible to reconstruct their class in a reliable way.
> Moreover, note that we have been able to create an object in blockchain
> without sending the bytecode for its code: that bytecode was
> already installed, at `takamakaCode`, and we do not
> need to repeat it every time we instantiate an object. Instead, the new object
> will refer to the jar that contains its bytecode.

In the following, you can use the `moka objects show` command on any object,
not just on your own accounts, whenever you want to inspect its state
(that includes the state inherited from its superclasses).

 <p align="center"><img width="850" src="pics/state2.png" alt="Figure 7. The state of the test network nodes after the creation of our new account"></p><p align="center">Figure 7. The state of the test network nodes after the creation of our new account.</p>


Figure 7 shows the state of the network nodes after the creation of our new account.
Since out test node is part of a blockchain, it is not only its state that has been modified,
but also that of all nodes that are part of that blockchain.

Whenever your account will run out of coins, you can recharge it with the
`moka send` command, using, again, the faucet as source of coins. Namely,
if you want to recharge your account with 200000 extra coins, you can type:

```shell
$ moka accounts send faucet 200000
    cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    --uri ws://panarea.hotmoka.io:8001
Adding transaction f739e34d3209b3f32369f43bc4c7129fad780a41dc4b7bbb78b51d70bdc4fc14... done.
```
You can then use the `moka objects show` command to verify that the balance of
your account has been actually increased with 200000 extra coins.

The creation of a new account from the faucet is possible from the Mokito app as well.
Namely, use the menu of the app to tap on the _Accounts_ item to see the
list of available accounts (Figure 2). From there, tap on the
menu icon on the right of the _Faucet_ account and select _Create a new account_
(see Figure 8).

 <p align="center"><img width="300" src="pics/mokito_new_account.png" alt="Figure 8. The menu for creating a new account with Mokito"></p><p align="center">Figure 8. The menu for creating a new account with Mokito.</p>


A form will appear, where you can specify the
name for the account, its password and the initial balance (that will be paid by the faucet).
For instance, you can fill it as in Figure 9.

 <p align="center"><img width="300" src="pics/mokito_elvis_new_account.png" alt="Figure 9. The form specifying a new account Elvis"></p><p align="center">Figure 9. The form specifying a new account Elvis.</p>


> The name of the accounts is a feature of Mokito to simplify the identification
> of the accounts. However, keep in mind that accounts have no name in Hotmoka: they
> are just identified by their storage reference. For instance, `moka` currently does not
> allow one to associate names to accounts.

After tapping on the `Create new account` button, the new account will be created and
its information will be shown, as in Figure 10. Again, note in this screen the storage reference of the new account
and the presence of a 36 words passphrase.

 <p align="center"><img width="300" src="pics/mokito_show_elvis.png" alt="Figure 10. The new account Elvis"></p><p align="center">Figure 10. The new account Elvis.</p>


If you go back to the accounts screen (by using the top-left menu of Mokito), you will see that Elvis
has been added to your accounts (see Figure 11).

 <p align="center"><img width="300" src="pics/mokito_added_elvis.png" alt="Figure 11. The new account Elvis has been imported"></p><p align="center">Figure 11. The new account Elvis has been imported.</p>


## Importing Accounts

We have created `cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0` with `moka` and
`701e20be588db820744df467826d67b9fe451406d7f75da6ef8aeb6805a7365f#0` with Mokito. We might want to _import_ the former in Mokito and the latter
in `moka`, so that we can operate on both accounts with both tools. In order to import
`701e20be588db820744df467826d67b9fe451406d7f75da6ef8aeb6805a7365f#0` in `moka`, we can use the `moka keys import` command and insert its 36 words
passphrase:


```shell
$ moka keys import around route kit grit ceiling electric negative
    nice pact dad forum real acid aware west balance return admit
    beach trip join cute page divert eagle parent remove upgrade
    surprise jelly close home aisle defy obey method
The key pair of the account has been imported
  into "701e20be588db820744df467826d67b9fe451406d7f75da6ef8aeb6805a7365f#0.pem".
```

 <p align="center"><img width="300" src="pics/mokito_accounts_menu.png" alt="Figure 12. The menu of the accounts screen"></p><p align="center">Figure 12. The menu of the accounts screen.</p>


 <p align="center"><img width="300" src="pics/mokito_insert_passphrase.png" alt="Figure 13. Inserting the 36 words passphrase in Mokito"></p><p align="center">Figure 13. Inserting the 36 words passphrase in Mokito.</p>


 <p align="center"><img width="300" src="pics/mokito_added_the_boss.png" alt="Figure 14. The new account The Boss has been imported"></p><p align="center">Figure 14. The new account The Boss has been imported.</p>


After that, it is possible to control that account with `moka` (if we remember
its password, that is, `chocolate`).

Vice versa, in order to import into Mokito the account that was created with `moka`,
first export the 36 words of that account:

```shell
$ moka keys export cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
The following BIP39 words represent the key pair of the account:
 1: jaguar
 2: young
 3: trial
 4: stick
 5: inmate
 6: tank
 7: earth
 8: rather
 9: echo
10: point
11: print
12: moon
13: winter
14: wrestle
15: solid
16: green
17: vapor
18: satisfy
19: despair
20: tuna
21: miracle
22: social
23: dilemma
24: repair
25: unfair
26: solar
27: butter
28: skate
29: lyrics
30: village
31: voyage
32: produce
33: reform
34: collect
35: machine
36: regret
```

Then go to the accounts page of Mokito and show its
top-right menu and select _Import account_ (see Figure 12).
In the screen that will appear, insert the name that you want to give to the account,
its password and its 36 words passphrase
(Figure 13).
Tap on the _Import Account_ button. The new account will show in the list of available accounts
(Figure 14). From this moment, it will be possible to control the account
from Mokito.

As you have seen, the 36 words of an account are enough for moving accounts around
different clients. Note that clients do not _contain_ accounts but only the cryptographic information
needed to access the accounts. If a client is uninstalled, the accounts that it used still exist in the remote
Hotmoka node and can still be re-imported and used
in some other client, if we have written down their 36 words.

## Anonymous Payments

The fact that accounts in Hotmoka are not just identified by their public key, but also by their
storage reference inside the state of a node, makes it a bit more difficult, but not impossible,
to execute anonymous transactions. We do not advocate the use of anonymity here, but it is true
that, sometimes, one wants to remain anonymous and still receive a payment.

> Anonymity is often used for illegal actions such as ransomware and blackmailing.
> We are against such actions. This section simply shows that anonymity can be achieved
> in Hotmoka as well, although it is a bit harder than with other blockchains.

Suppose for instance that somebody, whom we call Anonymous, wants to receive from us a payment
of 10,000 coins, but still wants to remain unknown. He can receive the payment in many ways:

1. He could send us an anonymous email asking us to pay to a specific account, already existing
   in the state of the node. But this is not anonymous, since, in Hotmoka, an account is an object
   and there must have been a transaction that created that object, whose payer is likely to be
   Anonymous or somebody in his clique. That is, this allows one to infer something
   about the identity of Anonymous. Therefore, Anonymous would probably discard this possibility.
2. He could send us an anonymous email asking
   us to create a new account with a given public key, whose associated
   private key he controls, and to charge it with 10,000 coins. After that, we are expected
   to send him an email where we notify him the storage reference where
   `moka accounts create` has allocated the account. But this means that we must know his
   email address, which is definitely against the idea of anonymity. Therefore,
   Anonymous discards this possibility as well.
3. He could send us an anonymous email asking us _to pay to a given public key_, whose
   associated private key he controls.
   After we pay to that key, he autonomously and anonymously recovers the storage reference of the
   resulting account, without any interaction with us. This is definitely anonymous and that is
   the technique that Anonymous will choose.

Let us show how the third possibility works. Anonymous starts by creating a new private/public key,
exactly as we did before:

```shell
$ moka keys create --name=anonymous.pem --password
Enter value for --password
  (the password that will be needed later to use the key pair): kiwis
The new key pair has been written into "anonymous.pem":
* public key: GMzD5Kar3jcpJu3r53cSj8pg4u9VKZ9vhC86J9DknTZK (ed25519, base58)
* public key: 5D+bBtbBp2M7+LBh71AEy7EGOetL6LgiJbnGv/juhrI= (ed25519, base64)
* Tendermint-like address: D3F001754E7F2554D6D0F6D4CFD82B7466B71FFF
```
Note that there is no `--uri` part in the `moka keys create` command, since this operation
runs completely off-line: no object gets created in the state of any Hotmoka node for now.
Anonymous pastes the new key into an anonymous email message to us:

```
Please pay 10000 coins to the key GMzD5Kar3jcpJu3r53cSj8pg4u9VKZ9vhC86J9DknTZK.
```

Once we receive that email, we use (for instance) our previous account to send 10000 coins to that key:

```shell
$ moka accounts send cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    10000 GMzD5Kar3jcpJu3r53cSj8pg4u9VKZ9vhC86J9DknTZK
    --password-of-payer
    --uri ws://panarea.hotmoka.io:8001
Enter value for --password-of-sender (the password of the sender): chocolate
Adding transaction 17e482bc136a106652059619dc0cb8ce199079d051e3f0fea70f50827464d6dc... done.
The payment went to
  account 17e482bc136a106652059619dc0cb8ce199079d051e3f0fea70f50827464d6dc#0.
The owner of the destination key pair can bind it now to its address with:
  moka keys bind file_containing_the_destination_key_pair
    --password --uri uri_of_this_Hotmoka_node
or with:
  moka keys bind file_containing_the_destination_key_pair
    --password --reference 17e482bc136a106652059619dc0cb8ce199079d051e3f0fea70f50827464d6dc#0

Gas consumption:
 * total: 7281
   * for CPU: 2756
   * for RAM: 4150
   * for storage: 375
   * for penalty: 0
 * price per unit: 1 pana
 * total price: 7281 panas
```

And that's all! No interaction is needed with Anonymous. He will check
from time to time to see if we have paid, by running the command `moka keys bind`
until it succeeds:

```shell
$ moka keys bind anonymous.pem --password --uri ws://panarea.hotmoka.io:8001
Cannot bind: nobody has paid anonymously to the key anonymous.pem up to now.

$ moka keys bind anonymous.pem --password --uri ws://panarea.hotmoka.io:8001
Cannot bind: nobody has paid anonymously to the key anonymous.pem up to now.

$ moka keys bind anonymous.pem --password --uri ws://panarea.hotmoka.io:8001
Enter value for --password (the password of the key pair): kiwis
The key pair of 17e482bc136a106652059619dc0cb8ce199079d051e3f0fea70f50827464d6dc#0
  has been saved as "17e482bc136a106652059619dc0cb8ce199079d051e3f0fea70f50827464d6dc#0.pem".
```
Once `moka keys bind` succeeds, Anonymous can enjoy his brand new account, that he
can control with the `kiwis` password.

So how does that work? The answer is that the `moka accounts send` command
creates the account `17e482bc136a106652059619dc0cb8ce199079d051e3f0fea70f50827464d6dc#0` with the public key of
Anonymous inside it, so that Anonymous will be able to control that account.
But there is more: that command
will also associate the public key of the account to the account itself,
inside a hash map contained in the manifest of the node,
called _accounts ledger_. The `moka keys bind` command will simply consult the
accounts ledger, to see if somebody has already bound an account to that public key.

> If, inside the accounts ledger, there is an account _C_ already associated to the
> public key,
> then the `moka accounts send` command will not create a new account but will increase the
> balance of _C_ and the `moka keys bind` command will consequently yield _C_.
> This is a security measure in order
> to avoid payment disruptions due to the association of dummy accounts to some keys
> or to repeated payments to the same key.
> In any case, the public key of _C_ can only be `5D+bBtbBp2M7+LBh71AEy7EGOetL6LgiJbnGv/juhrI=`, since the accounts ledger
> enforces that constraint when it gets populated with accounts:
> if somebody associates a key _K_ to an account _C_, then the public key
> contained inside _C_ must be _K_.

Anonymous payments are possible with Mokito as well. That client
allows one to create a key and pay to a key.

Should one use anonymous payments, always? The answer is no, since
anonymity comes with an extra gas cost: that for modifying the accounts ledger.
If there is no explicit need for anonymity, it is cheaper to receive payments
as described in points 1 and 2 above, probably without the need of anonymous emails.

## Installation of the Source Code

You will *not* need to download and install to source code of Hotmoka in this
book. Nevertheless, an important aspect of blockchain technology is that
trust is based also on the public availability of its code.
Moreover, you will need to download the source code if you want to understand
its inner working, or contribute to the development of the project or
fork the project.

Hence, we show below how to download the source code
of Hotmoka and of the runtime of the Takamaka language.
You will need Java JDK version at least 21.

Clone the project with:

```shell
$ git clone --branch v1.9.0 https://github.com/Hotmoka/hotmoka.git
```

then `cd` to the `hotmoka` directory and
compile, package, test and install the Hotmoka jars:

```shell
$ mvn clean install
```

All tests should pass and all projects should be successfully installed:

```
[INFO] Hotmoka parent ..................................... SUCCESS [  3.234 s]
[INFO] io-hotmoka-whitelisting-api ........................ SUCCESS [  1.154 s]
[INFO] io-hotmoka-whitelisting ............................ SUCCESS [  1.459 s]
[INFO] io-hotmoka-verification-api ........................ SUCCESS [  1.437 s]
[INFO] io-hotmoka-verification ............................ SUCCESS [  2.214 s]
[INFO] io-hotmoka-instrumentation-api ..................... SUCCESS [  1.158 s]
[INFO] io-hotmoka-instrumentation ......................... SUCCESS [  1.665 s]
...
[INFO] io-hotmoka-examples ................................ SUCCESS [  1.691 s]
[INFO] io-hotmoka-tests ................................... SUCCESS [04:02 min]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  04:25 min
[INFO] Finished at: 2024-03-08T18:57:52+02:00
[INFO] ------------------------------------------------------------------------
```

> If you are not interested in running the tests, append `-DskipTests` after
> the word `install`.

 <p align="center"><img width="450" src="pics/projects.png" alt="Figure 15. The Eclipse projects of Hotmoka"></p><p align="center">Figure 15. The Eclipse projects of Hotmoka.</p>


If you want to edit the source code inside an IDE, you can import it in Eclipse, NetBeans or IntelliJ.
In Eclipse, use the File &rarr; Import &rarr; Existing Maven Projects menu item and import
the parent Maven project contained in the `hotmoka` directory that you cloned from
GitHub. This should create, inside Eclipse, also its submodules.
You should see, inside Eclipse's project explorer, something like Figure 15.
You will then be able to compile, package, test and install the Hotmoka jars inside
Eclipse itself, by right-clicking on the `parent` project and selecting
`Run As` and then the `Mavel install` target. You will also be able to run the tests inside
the Eclipse JUnit runner, by right-clicking on the `io-hotmoka-tests` subproject
and selecting `Run As` and then the `JUnit Test` target.

# A First Takamaka Program

Takamaka is the language that can be used to write
smart contracts for Hotmoka nodes. Hotmoka
nodes and Takamaka code have exactly the same
relation as Ethereum nodes and Solidity code.

Let us start from a simple example of Takamaka code. Since we are
writing Java code, there is nothing special to learn or install
before starting writing programs in Takamaka. Just use your
preferred integrated development environment (IDE) for Java. Or even
do everything from command-line, if you prefer. Our examples below will be
shown for the Eclipse IDE, using Java 21 or later, but you can perfectly well
use the IntelliJ IDE instead.

Our goal will be to create a Java class that we will instantiate
and use in blockchain. Namely, we will learn how to create an object
of that class, that will be persisted in blockchain, and how we can later
call the `toString()` method on that instance in blockchain.

## Creation of the Eclipse Project

__[See `io-hotmoka-tutorial-examples-family` in `https://github.com/Hotmoka/hotmoka`]__

Let us create a Maven project `io-hotmoka-tutorial-examples-family` inside Eclipse,
in the `hotmoka_tutorial` directory.
For that, in the Eclipse's Maven wizard
(New &rarr; Maven project) specify the options
*Create a simple project (skip archetype selection)*
and deselect the *Use default Workspace directory* option,
specifying a subdirectory `io-hotmoka-tutorial-examples-family` of the `hotmoka_tutorial` directory as *Location* instead.
Hence, *Location* should be something that ends with `.../hotmoka_tutorial/io-hotmoka-tutorial-examples-family`.
Do not add the project to any working set. Use `io.hotmoka`
as Group Id and `io-hotmoka-tutorial-examples-family` as Artifact Id.

> The Group Id can be changed as you prefer, but we will stick
> to `io.hotmoka` to show the exact files that you will see in the provided code.

By clicking *Finish* in the Eclipse's Maven wizard, you should see
a new Maven project in the Eclipse's explorer.
Currently, Eclipse creates a default `pom.xml` file that uses Java 5
and has no dependencies. Replace hence
the content of the `pom.xml` file of the `io-hotmoka-tutorial-examples-family` project with the code that follows:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                        http://maven.apache.org/xsd/maven-4.0.0.xsd">

  <modelVersion>4.0.0</modelVersion>
  <groupId>io.hotmoka</groupId>
  <artifactId>io-hotmoka-tutorial-examples-family</artifactId>
  <version>1.9.0</version>

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.release>21</maven.compiler.release>
  </properties>

  <dependencies>
    <dependency>
      <groupId>io.hotmoka</groupId>
      <artifactId>io-takamaka-code</artifactId>
      <version>1.5.0</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.11.0</version>
      </plugin>
    </plugins>
  </build>

</project>
```

It specifies to use Java 21 and provides the dependency
to `io-takamaka-code`, that is, the run-time classes of the Takamaka smart contracts.

> We are using `1.5.0` here, as version of the Takamaka runtime
> project. You can replace that, if needed, with the latest version of the project.

Since the `pom.xml` file has changed, Eclipse will normally show an error
in the project. To solve it,
you need to update the Maven dependencies of the project:
right-click on the project &rarr; Maven &rarr; Update Project...

As you can see, we are importing the dependency `io-takamaka-code`,
that contains the Takamaka runtime. This will be downloaded from Maven
and everything should compile without errors.
The result in Eclipse should look similar to what is
shown in Figure 16.

 <p align="center"><img width="280" src="pics/family.png" alt="Figure 16. The family Eclipse project"></p><p align="center">Figure 16. The family Eclipse project.</p>


Create a `module-info.java` file inside `src/main/java`
(right-click on the project &rarr; Configure &rarr; Create module-info.java &rarr; Create),
to state that this project depends on the module containing the runtime of Takamaka:

```java
module family {
  requires io.takamaka.code;
}
```

Create a package `io.hotmoka.tutorial.examples.family` inside `src/main/java`. Inside that package,
create a Java source `Person.java`, by copying and pasting the following code:

```java
package io.hotmoka.tutorial.examples.family;

import io.takamaka.code.lang.StringSupport;

public class Person {
  private final String name;
  private final int day;
  private final int month;
  private final int year;
  public final Person parent1;
  public final Person parent2;

  public Person(String name, int day, int month, int year,
                Person parent1, Person parent2) {

    this.name = name;
    this.day = day;
    this.month = month;
    this.year = year;
    this.parent1 = parent1;
    this.parent2 = parent2;
  }

  public Person(String name, int day, int month, int year) {
    this(name, day, month, year, null, null);
  }

  @Override
  public String toString() {
    return StringSupport.concat(name, " (", day, "/", month, "/", year, ")");
  }
}
```

This is a plain old Java class and should not need any comment. The only observation is that we concat strings
with the support class `StringSupport`, since the standard string concatenation of Java would end up
calling methods whose computational cost is not foreseeable in advance. In general, a very small portion of the
Java library can be used directly in Takamaka, and support classes are used to replace some common functionalities,
such as string concatenation.

Package the project into a jar and install it in the local Maven repository, by running the following shell command inside
the directory of the project (that is, the subdirectory `io-hotmoka-tutorial-examples-family` of the
directory `hotmoka_tutorial`):

```shell
$ mvn install
```

A `io-hotmoka-tutorial-examples-family-1.9.0.jar` file should appear inside the `target` directory.
Only the compiled
class files will be relevant: Hotmoka nodes will ignore source files, manifest
and any resources in the jar; the same compiled
`module-info.class` is irrelevant for Hotmoka.
All such files can be removed from the jar, to reduce the gas cost of their
installation in the store of a node, but we do not care about this optimization here.
The result should look as in Figure 17:

 <p align="center"><img width="300" src="pics/family_jar.png" alt="Figure 17. The family Eclipse project, exported in jar"></p><p align="center">Figure 17. The family Eclipse project, exported in jar.</p>


## Installation of the Jar in a Hotmoka Node

__[See `io-hotmoka-tutorial-examples-runs` in `https://github.com/Hotmoka/hotmoka`]__

We have generated the jar containing our code and we want to send it now to a Hotmoka node,
where it will be installed. This means that it will become available to programmers
who want to use its classes, directly or as dependencies of their programs.
In order to install a jar in the Hotmoka node that we have used in the previous chapter,
we can use the `moka` command-line tool, specifying which account will pay for the
installation of the jar. The cost of the installation depends on the size of the
jar and on the number of its dependencies. The `moka` tool uses a heuristics to
foresee the cost of installation. Move inside the `hotmoka_tutorial` directory, if you are not
there already, so that
`moka` will find your saved key pair there, and run the `moka jars install` command:

```shell
$ cd hotmoka_tutorial
$ moka jars install cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    io-hotmoka-tutorial-examples-family/target/io-hotmoka-tutorial-examples-family-1.9.0.jar
    --password-of-payer
    --uri ws://panarea.hotmoka.io:8001

Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate
Adding transaction e33816ed24cd5329b9aed17dcfc1f55ae5993ce72bdad30f5a9e3ad9e2414a29... done.
The jar has been installed at e33816ed24cd5329b9aed17dcfc1f55ae5993ce72bdad30f5a9e3ad9e2414a29.

Gas consumption:
 * total: 9330
   * for CPU: 1618
   * for RAM: 3308
   * for storage: 4404
   * for penalty: 0
 * price per unit: 1 pana
 * total price: 9330 panas
```

As you can see above, the jar has been installed at a reference, that can be used
later to refer to that jar. This has costed some gas, paid by our account.
You can verify that the balance of the account has been decreased, through the
`moka objects show` command.

The state of the Hotmoka nodes of the testnet is now as in Figure 18.
As that figure shows, a dependency has been created, automatically, from `io-hotmoka-tutorial-examples-family-1.9.0.jar` to
`io-takamaka-code-1.5.0.jar`. This is because all Takamaka code will use the run-time classes of the Takamaka language,
hence the `moka jars install` command adds them, by default. Note that a dependency must already be installed in the node
before it can be used as dependency of other jars.

 <p align="center"><img width="850" src="pics/state3.png" alt="Figure 18. The state of the test network nodes after the installation of our jar"></p><p align="center">Figure 18. The state of the test network nodes after the installation of our jar.</p>


What we have done above is probably enough for most users, but sometimes you need
to perform the same operation in code, for instance in order to implement a software
application that connects to a Hotmoka node and runs some transactions.
Therefore, we describe below how you can write a Java program that installs the
same jar in the Hotmoka node, without using the `moka jars install` command.
A similar translation in code can be performed for all examples in this tutorial,
but we will report it only for a few of them.

Let us hence create another Eclipse Maven project
`io-hotmoka-tutorial-examples-runs`, inside `hotmoka_tutorial`,
exactly as we did in the previous section for the `family` project.
Specify Java 21 (or later) in its build configuration.
Use `io.hotmoka` as Group Id and `io-hotmoka-tutorial-examples-runs` as Artifact Id.
This is specified in the following `pom.xml`, that you should copy inside
the `io-hotmoka-tutorial-examples-runs` project, replacing that generated by Eclipse:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                      http://maven.apache.org/xsd/maven-4.0.0.xsd">

  <modelVersion>4.0.0</modelVersion>
  <groupId>io.hotmoka</groupId>
  <artifactId>io-hotmoka-tutorial-examples-runs</artifactId>
  <version>1.9.0</version>

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.release>21</maven.compiler.release>
  </properties>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.11.0</version>
      </plugin>
    </plugins>
  </build>

  <dependencies>
    <dependency>
      <groupId>io.hotmoka</groupId>
      <artifactId>io-hotmoka-node-remote</artifactId>
      <version>1.9.0</version>
    </dependency>
    <dependency>
      <groupId>io.hotmoka</groupId>
      <artifactId>io-hotmoka-helpers</artifactId>
      <version>1.9.0</version>
    </dependency>
	<dependency>
      <groupId>io.hotmoka</groupId>
      <artifactId>io-hotmoka-node-tendermint</artifactId>
      <version>1.9.0</version>
    </dependency>
    <dependency>
      <groupId>io.hotmoka</groupId>
      <artifactId>io-hotmoka-node-disk</artifactId>
      <version>1.9.0</version>
    </dependency>
    <dependency>
      <groupId>io.hotmoka</groupId>
      <artifactId>io-hotmoka-node-service</artifactId>
      <version>1.9.0</version>
    </dependency>
    <dependency>
      <groupId>io.hotmoka</groupId>
      <artifactId>io-hotmoka-constants</artifactId>
      <version>1.9.0</version>
    </dependency>
    <dependency>
      <groupId>io.hotmoka</groupId>
      <artifactId>io-takamaka-code-constants</artifactId>
      <version>1.5.0</version>
    </dependency>
  </dependencies>

</project>
```

This `pom.xml` specifies a few dependencies. We do not need all of them now,
but we will need them along the next sections, hence let us insert them all already.
These dependencies get automatically downloaded from the Maven repository.

Since we modified the file `pom.xml`, Eclipse could show an error
for the `io-hotmoka-tutorial-examples-runs` project. To fix it,
you need to update the Maven dependencies of the project:
right-click on the `io-hotmoka-tutorial-examples-runs` project &rarr; Maven &rarr; Update Project...

The result should look as in Figure 19.

 <p align="center"><img width="300" src="pics/runs.png" alt="Figure 19. The `io-hotmoka-tutorial-examples` Eclipse project"></p><p align="center">Figure 19. The `io-hotmoka-tutorial-examples` Eclipse project.</p>


Create a `module-info.java` inside `src/main/java`, containing:

```java
module io.hotmoka.tutorial.examples.runs {
  requires io.hotmoka.helpers;
  requires io.hotmoka.node.remote;
  requires io.hotmoka.node.disk;
  requires io.hotmoka.node.tendermint;
  requires io.hotmoka.node.service;
  requires io.hotmoka.constants;
  requires io.takamaka.code.constants;
}
```

Again, we do not need all these dependencies already, but we will need them later.

Create a package
`io.hotmoka.tutorial.examples.runs` inside `src/main/java` and add the following class `Family.java` inside it:

```java
package io.hotmoka.tutorial.examples.runs;

import static java.math.BigInteger.ONE;

import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;

import io.hotmoka.constants.Constants;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.RemoteNodes;

public class Family {

  public static void main(String[] args) throws Exception {

	// the path of the user jar to install
   var familyPath = Paths.get(System.getProperty("user.home")
     + "/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family/"
     + Constants.HOTMOKA_VERSION
     + "/io-hotmoka-tutorial-examples-family-" + Constants.HOTMOKA_VERSION + ".jar");

	var dir = Paths.get(args[1]);
	var payer = StorageValues.reference(args[2]);
	var password = args[3];

	try (var node = RemoteNodes.of(new URI(args[0]), 80000)) {
    	// we get a reference to where io-takamaka-code-X.Y.Z.jar has been stored
      TransactionReference takamakaCode = node.getTakamakaCode();

      // we get the signing algorithm to use for requests
      var signature = node.getConfig().getSignatureForRequests();

      KeyPair keys = loadKeys(node, dir, payer, password);

      // we create a signer that signs with the private key of our account
      Signer<SignedTransactionRequest<?>> signer = signature.getSigner
        (keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);

      // we get the nonce of our account: we use the account itself as caller and
      // an arbitrary nonce (ZERO in the code) since we are running
      // a @View method of the account
      BigInteger nonce = node
        .runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
          (payer, // payer
          BigInteger.valueOf(100_000), // gas limit
          takamakaCode, // class path for the execution of the transaction
          MethodSignatures.NONCE, // method
          payer)).get() // receiver of the method call
        .asBigInteger(__ -> new ClassCastException());

      // we get the chain identifier of the network
      String chainId = node.getConfig().getChainId();

      var gasHelper = GasHelpers.of(node);

      // we install the family jar in the node: our account will pay
      TransactionReference family = node
        .addJarStoreTransaction(TransactionRequests.jarStore
          (signer, // an object that signs with the payer's private key
          payer, // payer
          nonce, // payer's nonce: relevant since this is not a call to a @View method!
          chainId, // chain identifier: relevant since this is not a call to a @View method!
          BigInteger.valueOf(300_000), // gas limit: enough for this very small jar
          gasHelper.getSafeGasPrice(), // gas price: at least the current gas price of the network
          takamakaCode, // class path for the execution of the transaction
          Files.readAllBytes(familyPath), // bytes of the jar to install
          takamakaCode)); // dependencies of the jar that is being installed

      // we increase our copy of the nonce, ready for further
      // transactions having the account as payer
      nonce = nonce.add(ONE);

      System.out.println("jar installed at " + family);
    }
  }

  private static KeyPair loadKeys(Node node, Path dir, StorageReference account, String password)
      throws Exception {

    return Accounts.of(account, dir)
      .keys(password, SignatureHelpers.of(node).signatureAlgorithmFor(account));
  }
}
```
As you can see, the above `main` method requires three values to be provided from the caller:
the directory `dir` where the key pair of the payer account can be found,
the storage reference of that payer account, and the password of the key pair.

The code above creates an instance of a `RemoteNode`, that represents a Hotmoka
node installed in a remote host. By specifying the URI of the host, the `RemoteNode` object
exposes all methods of a Hotmoka node. It is an `AutoCloseable` object, hence it is placed inside
a try-with-resource statement that guarantees its release at the end of the `try` block.
By using that remote node, our code collects
some information about the node: the reference to the `io-takamaka-code` jar already
installed inside it (`takamakaCode`) and the signature algorithm used by the node (`signature`),
that it uses to construct a `signer` object that signs with the private key of our account,
loaded from disk.

The `loadKeys` method accesses the `.pem` file that we have previously created
with `moka`, that should be inside a directory `dir`.

Like every Hotmoka node, the observable state of the remote node can only evolve through
*transactions*, that modify its state in an atomic way.
Namely, the code above performs two transactions:

1. A call to the `nonce()` method of our account: this is a progressive counter of the number
   of transactions already performed with our account. It starts from zero, but our account has been
   already used for other transactions (through the `moka` tool). Hence we better ask the
   node about it. As we will see later, this transaction calls a `@View` method. All calls
   to `@View` methods have the nice feature of being *for free*: nobody will pay for them.
   Because of that, we do not need to sign this transaction, or to provide a correct nonce,
   or specify a gas price. The limitation of such calls is that their transactions are not
   checked by consensus, hence we have to trust the node we ask. Moreover, they can only
   read, never write the data in the store of the node.
2. The addition of our jar in the node. This time the transaction has a cost and our
   account is specified as payer. The signer of our account signs the transaction.
   Nonce of our account and chain identifier of the network are relevant, as well as the
   gas price, that must at least match that of the network.
   The code uses the `addJarStoreTransaction()` method, that executes a new transaction
   on the node, whose goal is to install a jar inside it. The jar is provided as a sequence of bytes
   (`Files.readAllBytes(familyPath)`, where `familyPath` looks inside the local Maven repository
   of the machine, where the jar to install in the node should already be present.
   The request passed to `addJarStoreTransaction()` specifies that the transaction can cost up
   to 300,000 units of gas, that can be bought at the price returned by the `gasHelper` object. The request
   specifies that its class path is `node.getTakamakaCode()`: this is the reference to the
   `io-takamaka-code` jar already installed in the node.
   Finally, the request specifies that `io-hotmoka-tutorial-examples-family-1.9.0.jar` has only
   a single dependency: `io-takamaka-code`. This means that when, later, we will refer to
   `io-hotmoka-tutorial-examples-family-1.9.0.jar` in a class path, this class path will indirectly include its dependency
   `io-takamaka-code` as well (see Figure 18).

> As in Ethereum, transactions in Hotmoka are paid
> in terms of gas consumed for their execution.
> Calls to `@View` methods do not actually modify the state of the node
> and are executed locally, on the
> node that receives the request of the transaction. Hence, they can be considered
> as run *for free*. Instead, we have used an actual gas price for the last
> transaction that installs the jar in blockchain. This could be computed with
> a sequence of calls to `@View` methods (get the manifest, then the gas station
> inside the manifest, then the gas price inside the gas station). In order to
> simplify the code, we have used the `GasHelper` class, that does exactly that for us.

You can run the program with Maven, specifying the server to contact,
the class to run and the parameters to pass to its `main` method:
```shell
$ mvn compile exec:java -Dexec.mainClass="io.hotmoka.tutorial.examples.runs.Family"
     -Dexec.args="ws://panarea.hotmoka.io:8001
                  hotmoka_tutorial
                  cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
                  chocolate"
jar installed at: fa6c878210349d429a065199b5e28ce6e236962b6640f7ebdb3845b1617280ad
```
The exact address will change in your machine. In any case, note that this reference to the jar is functionally equivalent to that
obtained before with the `moka jars install` command: they point to equivalent jars.

## Creation of an Object of our Program

__[See `io-hotmoka-tutorial-examples-family_storage` in `https://github.com/Hotmoka/hotmoka`]__

__[See `io-hotmoka-tutorial-examples-runs` in `https://github.com/Hotmoka/hotmoka`]__

The jar of our program is in the store of the node now: the `moka jars install` command
has installed it at `e33816ed24cd5329b9aed17dcfc1f55ae5993ce72bdad30f5a9e3ad9e2414a29` and our code at `fa6c878210349d429a065199b5e28ce6e236962b6640f7ebdb3845b1617280ad`.
We can use either of them, interchangeably, as class path for the execution of a transaction that
tries to run the constructor of `Person` and add a brand
new `Person` object into the store of the node. We can perform this through the `moka` tool:

```shell
$ cd hotmoka_tutorial # if you are not already there
$ moka objects create cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    io.hotmoka.tutorial.examples.family.Person Einstein 14 4 1879 null null
    --classpath e33816ed24cd5329b9aed17dcfc1f55ae5993ce72bdad30f5a9e3ad9e2414a29
    --uri ws://panarea.hotmoka.io:8001 --password-of-payer
Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate
Do you really want to call constructor
  public ...Person(java.lang.String,int,int,int,...Person,...Person)
spending up to 200000 gas units at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y
Adding transaction e13885edde4ae93b1b66c906980d069f65cbc7237f7eca59b85759d5b8921ae8... failed.
The transaction failed with message io.hotmoka.node.api.SerializationException:
  An object of class ...Person cannot be serialized into a storage value
  since it does not implement io.takamaka.code.lang.Storage

Gas consumption:
 * total: 200000
   * for CPU: 1615
   * for RAM: 3144
   * for storage: 204
   * for penalty: 195037
 * price per unit: 1 pana
 * total price: 200000 panas
```

The `moka objects create` command requires to specify who pays for the object creation
(our account), then the fully-qualified name of the class that we want to instantiate
(`io.hotmoka.tutorial.examples.Person`) followed by the actual arguments passed to its constructor.
The classpath refers to the jar that we have installed previously. The `moka objects create` command
asks for the password of the payer account and
checks if we really want to proceed (and pay). Then it ends up in failure
(`SerializationException`). Note that all offered gas has been spent.
This is a sort of *penalty* for running a transaction that fails. The rationale is that this penalty should discourage
potential denial-of-service attacks, when a huge number of failing transactions are thrown at a
node. At least, that attack will cost a lot. Moreover, note that the transaction, although
failed, does exist. Indeed, the nonce of the caller has been increased, as you can check with `moka objects show`
on your account.

But we still have not understood why the transaction failed. The reason is in the exception
message: `An object of class ...Person cannot be serialized into a storage value since it does not implement io.takamaka.code.lang.Storage`.
Takamaka requires
that all objects stored in a node extend the `io.takamaka.code.lang.Storage` class. That superclass
provides all the machinery needed in order to keep track of updates to such objects and persist them
in the store of the node, automatically.

> Do not get confused here. Takamaka does **not** require all objects to extend
> `io.takamaka.code.lang.Storage`. You can use objects that do not extend that superclass in your
> Takamaka code, both instances of your classes and instances of library classes
> from the `java.*` hierarchy, for instance. What Takamaka does require, instead, is that objects
> _that must be kept in the store of a node_ do extend `io.takamaka.code.lang.Storage`. This
> must be the case, for instance, for objects created by the constructor invoked through the
> `moka objects create` command.

Let us modify the `io.hotmoka.tutorial.examples.Person.java` source code, inside the `io-hotmoka-tutorial-examples-family` project then:

```java
package io.hotmoka.tutorial.examples.family;

import io.takamaka.code.lang.Storage;

public class Person extends Storage {
  ... unchanged code ...
}
```

> Extending `io.takamaka.code.lang.Storage` is all a programmer needs to do in order to let instances
> of a class be stored in the store of a node. There is no explicit method to call to keep track
> of updates to such objects and persist them in the store of the node:
> Hotmoka nodes will automatically deal with them.

> We can use the `io.takamaka.code.lang.Storage` class and we can run the resulting compiled code
> since that class is inside `io-takamaka-code`, that has been included in the
> class path as a dependency of `io-hotmoka-tutorial-examples-family-1.9.0.jar`.

Regenerate `io-hotmoka-tutorial-examples-family-1.9.0.jar`, by running `mvn install` again,
inside the `io-hotmoka-tutorial-examples-family` project, since class `Person` has changed.
Then run again the `moka objects create` command. This time, the execution should
complete without exception:

```shell
$ cd io-takamaka-code-examples-family
$ mvn clean install
$ cd ..
$ moka jars install cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    io-hotmoka-tutorial-examples-family/target/io-hotmoka-tutorial-examples-family-1.9.0.jar
    --password-of-payer
    --uri ws://panarea.hotmoka.io:8001
...
has been installed at
  68c769d16b23b55c3b15db990ed22c561155035bd3c696d3fb720d17e0d39a80
...
$ moka objects create cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    io.hotmoka.tutorial.examples.family.Person Einstein 14 4 1879 null null
    --classpath 68c769d16b23b55c3b15db990ed22c561155035bd3c696d3fb720d17e0d39a80
    --uri ws://panarea.hotmoka.io:8001 --password-of-payer
Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate
Do you really want to call constructor
  public ...Person(java.lang.String,int,int,int,...Person,...Person)
spending up to 200000 gas units at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y
Adding transaction cac4d52b4fb709e91712e56f0ca1ade040623079fd9f20ab8b9416db74964666... done.
A new object cac4d52b4fb709e91712e56f0ca1ade040623079fd9f20ab8b9416db74964666#0 has been created.

Gas consumption:
 * total: 5210
   * for CPU: 1618
   * for RAM: 3157
   * for storage: 435
   * for penalty: 0
 * price per unit: 1 pana
 * total price: 5210 panas
```

The new object has been allocated at a storage reference that can be used
to refer to it, also in the future:
`cac4d52b4fb709e91712e56f0ca1ade040623079fd9f20ab8b9416db74964666#0`.
You can verify that it is actually there and that its fields are correctly initialized,
by using the `moka objects show` command:

```shell
$ cd hotmoka_tutorial
$ moka objects show cac4d52b4fb709e91712e56f0ca1ade040623079fd9f20ab8b9416db74964666#0
    --uri ws://panarea.hotmoka.io:8001

class io.hotmoka.tutorial.examples.family.Person
    (from jar installed at 68c769d16b23b55c3b15db990ed22c561155035bd3c696d3fb720d17e0d39a80)
  day:int = 14
  month:int = 4
  name:java.lang.String = "Einstein"
  parent1:...Person = null
  parent2:...Person = null
  year:int = 1879
```

> Compared with Solidity, where contracts and accounts are just untyped *addresses*,
> objects (and hence accounts) are strongly-typed in Takamaka.
> This means that they are tagged with their run-time type (see the output
> of `moka objects show` above), in a boxed representation,
> so that it is possible to check that they are used correctly, ie., in accordance
> with the declared type of variables, or to check their run-time type with checked casts
> and the `instanceof` operator. Moreover, Takamaka has information to check
> that such objects have been created by using the same
> jar that stays in the class path later, every time an object gets used
> (see the information `from jar installed at` in the output of `moka objects show` above).

We can perform the same object creation in code, instead of using the `moka objects create` command.
Namely, the following code builds on the previous example and installs a jar by adding
a further transaction that calls the constructor of `Person`:

```java
package io.hotmoka.tutorial.examples.runs;

import static io.hotmoka.helpers.Coin.panarea;
import static io.hotmoka.node.StorageTypes.INT;
import static java.math.BigInteger.ONE;

import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;

import io.hotmoka.constants.Constants;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.RemoteNodes;

public class FamilyStorage {

  private final static ClassType PERSON = StorageTypes.classNamed
    ("io.hotmoka.tutorial.examples.family.Person");

  public static void main(String[] args) throws Exception {

	 // the path of the user jar to install
    var familyPath = Paths.get(System.getProperty("user.home")
      + "/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family/"
      + Constants.HOTMOKA_VERSION
      + "/io-hotmoka-tutorial-examples-family-" + Constants.HOTMOKA_VERSION + ".jar");

    var dir = Paths.get(args[1]);
    var payer = StorageValues.reference(args[2]);
    var password = args[3];

    try (var node = RemoteNodes.of(new URI(args[0]), 80000)) {
      // we get a reference to where io-takamaka-code-X.Y.Z.jar has been stored
      TransactionReference takamakaCode = node.getTakamakaCode();

      // we get the signing algorithm to use for requests
      var signature = node.getConfig().getSignatureForRequests();

	   KeyPair keys = loadKeys(node, dir, payer, password);

      // we create a signer that signs with the private key of our account
      Signer<SignedTransactionRequest<?>> signer = signature.getSigner
        (keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);

      // we get the nonce of our account: we use the account itself as caller and
      // an arbitrary nonce (ZERO in the code) since we are running
      // a @View method of the account
      BigInteger nonce = node
        .runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
          (payer, // payer
          BigInteger.valueOf(100_000), // gas limit
          takamakaCode, // class path for the execution of the transaction
          MethodSignatures.NONCE, // method
          payer)).get() // receiver of the method call
        .asBigInteger(__ -> new ClassCastException());

      // we get the chain identifier of the network
      String chainId = node.getConfig().getChainId();

      var gasHelper = GasHelpers.of(node);

      // we install the family jar in the node: our account will pay
      TransactionReference family = node
        .addJarStoreTransaction(TransactionRequests.jarStore
          (signer, // an object that signs with the payer's private key
          payer, // payer
          nonce, // payer's nonce: relevant since this is not a call to a @View method!
          chainId, // chain identifier: relevant since this is not a call to a @View method!
          BigInteger.valueOf(300_000), // gas limit: enough for this very small jar
          gasHelper.getSafeGasPrice(), // gas price: at least the current gas price of the network
          takamakaCode, // class path for the execution of the transaction
          Files.readAllBytes(familyPath), // bytes of the jar to install
          takamakaCode)); // dependencies of the jar that is being installed

      // we increase our copy of the nonce, ready for further
      // transactions having the account as payer
      nonce = nonce.add(ONE);

      // call the constructor of Person and store in einstein the new object in blockchain
      StorageReference einstein = node.addConstructorCallTransaction
        (TransactionRequests.constructorCall
         (signer, // an object that signs with the payer's private key
         payer, // payer
         nonce, // payer's nonce: relevant since this is not a call to a @View method!
         chainId, // chain identifier: relevant since this is not a call to a @View method!
         BigInteger.valueOf(100_000), // gas limit: enough for a small object
         panarea(gasHelper.getSafeGasPrice()), // gas price, in panareas
         family, // class path for the execution of the transaction

         // constructor Person(String,int,int,int)
         ConstructorSignatures.of(PERSON, StorageTypes.STRING, INT, INT, INT),

         // actual arguments
         StorageValues.stringOf("Einstein"), StorageValues.intOf(14),
         StorageValues.intOf(4), StorageValues.intOf(1879)
      ));

      System.out.println("new object allocated at " + einstein);

      // we increase our copy of the nonce, ready for further
      // transactions having the account as payer
      nonce = nonce.add(ONE);
    }
  }

  private static KeyPair loadKeys(Node node, Path dir, StorageReference account, String password)
		  throws Exception {

    return Accounts.of(account, dir)
    		.keys(password, SignatureHelpers.of(node).signatureAlgorithmFor(account));
  }
}
```

The new transaction is due to the
`addConstructorCallTransaction()` method, that expands the node with a new transaction that calls
a constructor. We use our account as payer for the transaction, hence we sign
the request with its private key.
The class path includes `io-hotmoka-tutorial-examples-family-1.9.0.jar` and its dependency `io-takamaka-code`.
The signature of the constructor specifies that we are referring to the second
constructor of `Person`, the one that assumes `null` as parents. The actual parameters
are provided; they must be instances of the `io.hotmoka.node.api.values.StorageValue` interface.
We provide 50,000 units of gas, which should be enough for a constructor that just initializes a few fields.
We are ready to pay `panarea(gasHelper.getSafeGasPrice())` units of coin for each unit of gas.
This price could have been specified simply as `gasHelper.getSafeGasPrice()`
but we used the static method `io.hotmoka.helpers.Coin.panarea()`
to generate a `BigInteger` corresponding to the smallest coin unit of Hotmoka nodes, a *panarea*.
Namely, the following units of coin exist:

| Value (in panas)      | Exponent           | Name | Short Name |
| --------------------- |:-------------:| ----- | ----- |
| 1      | 1 | panarea | pana |
| 1,000  | 10<sup>3</sup> | alicudi | ali |
| 1,000,000 | 10<sup>6</sup> | filicudi | fili |
| 1,000,000,000 | 10<sup>9</sup> | stromboli | strom |
| 1,000,000,000,000 | 10<sup>12</sup> | vulcano | vul |
| 1,000,000,000,000,000 | 10<sup>15</sup> | salina | sali |
| 1,000,000,000,000,000,000 | 10<sup>18</sup> | lipari | lipa |
| 1,000,000,000,000,000,000,000 | 10<sup>21</sup> | moka | moka |

with corresponding static methods in `io.hotmoka.helpers.Coin`.

By running `FamilyStorage`, you should see the following on the console:
```
$ mvn compile exec:java
     -Dexec.mainClass="io.hotmoka.tutorial.examples.runs.FamilyStorage"
     -Dexec.args="ws://panarea.hotmoka.io:8001
                  hotmoka_tutorial
                  cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
                  chocolate"
new object allocated
  at b0876a380103a9693074daff3db6e3cb2c697047ff573008266dc6369181a8b3#0
```
The exact address will change at any run.

## Calling a Method on an Object in a Hotmoka Node

__[See `io-hotmoka-tutorial-examples-family_exported` in `https://github.com/Hotmoka/hotmoka`]__

__[See `io-hotmoka-tutorial-examples-runs` in `https://github.com/Hotmoka/hotmoka`]__

In the previous section, we have created an object of class `Person` in the store
of the node. Let us invoke the
`toString()` method on that object now. For that, we can use the `moka objects call` command,
specifying our `Person` object as *receiver*.

> In object-oriented languages, the _receiver_ of a call to a non-`static`
> method is the object over which the method is executed, that is accessible
> as `this` inside the code of the method. In our case, we want to invoke
> `einstein.toString()`, where `einstein` is the object that we have created
> previously, hence the receiver of the call.
> The receiver can be seen as an implicit actual argument passed to a
> (non-`static`) method.

```shell
$ moka objects call cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    io.hotmoka.tutorial.examples.family.Person toString --password-of-payer
    --receiver=cac4d52b4fb709e91712e56f0ca1ade040623079fd9f20ab8b9416db74964666#0
    --uri=ws://panarea.hotmoka.io:8001
Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate 
Do you really want to call method public java.lang.String ...Person.toString()
  spending up to 200000 gas units at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y
Adding transaction 796ebd74089ce7ad3caf7a3b572cf37349cf350ca3d5acd4300eb78f0674e607... rejected!
  [io.hotmoka.node.api.TransactionRejectedException:
    Class io.hotmoka.tutorial.examples.family.Person of the parameter
    cac4d52b4fb709e91712e56f0ca1ade040623079fd9f20ab8b9416db74964666#0 is not exported:
    add @Exported to io.hotmoka.tutorial.examples.family.Person]
```

Command `moka objects call` requires to specify, as its first arguments,
the payer of the call, that is, our account, followed by the name of the class whose
method is called and the name of that method. The receiver of the call is specified through
`--receiver`.

As you can see above, the result is deceiving.

This exception occurs when we try to pass the `Person` object
as receiver of `toString()` (the receiver is a particular case of an actual
argument). That object has been created in store, has escaped the node
and is available through its storage reference. However, it cannot be passed back
into the node as argument of a call since it is not _exported_. This is a security feature of
Hotmoka. Its reason is that the store of a node is public and can be read freely.
Everybody can see the objects created in the store of a Hotmoka node
and their storage references can be used to invoke their methods and modify their state.
This is true also for objects meant to be private state components of other objects and that
are not expected to be freely modifiable from outside the node. Because of this,
Hotmoka requires that classes, whose instances can be passed into the node as
arguments to methods or constructors,
must be annotated as `@Exported`. This means that the programmer acknowledges the
use of these instances from outside the node.

> Note that all objects can be passed, from _inside_ the blockchain, as arguments to methods
> of code in the node. The above limitation applies to objects passed from _outside_ the node only.

Let us modify the `Person` class again:

```java
...
import io.takamaka.code.lang.Exported;
...

@Exported
public class Person extends Storage {
  ...
}
```

Package the project `io-hotmoka-tutorial-examples-family` and try again to call the `toString` method:

```shell
$ cd io-hotmoka-tutorial-examples-family
$ mvn clean install
$ cd ..
$ moka jars install cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    io-hotmoka-tutorial-examples-family/target/io-hotmoka-tutorial-examples-family-1.9.0.jar
    --password-of-payer
    --uri ws://panarea.hotmoka.io:8001
...
jar installed at
  3c5f574e5bad97445437f0f4fc0d1d7ed35c58a4245fc6046238cb24a6a1d11d
...
$ moka objects create cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    io.hotmoka.tutorial.examples.family.Person
    Einstein 14 4 1879 null null
    --classpath 3c5f574e5bad97445437f0f4fc0d1d7ed35c58a4245fc6046238cb24a6a1d11d
    --uri ws://panarea.hotmoka.io:8001 --password-of-payer
Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate
Do you really want to call constructor
  public ...Person(java.lang.String,int,int,int,...Person,...Person)
spending up to 200000 gas units at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y
Adding transaction 657b600fca38a7d84a0abe447e750e453cbebb58fded6d2970421dba90e09202... done.
A new object 657b600fca38a7d84a0abe447e750e453cbebb58fded6d2970421dba90e09202#0 has been created.
...
$ moka objects call cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    io.hotmoka.tutorial.examples.family.Person
    toString
    --password-of-payer
    --receiver=657b600fca38a7d84a0abe447e750e453cbebb58fded6d2970421dba90e09202#0
    --uri=ws://panarea.hotmoka.io:8001
Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate
Do you really want to call method public java.lang.String ...Person.toString() spending up to 200000 gas units
  at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y
Adding transaction e44b3c2c6414af14d6f2379ff0908af3e9026a5e1881ef66621d6b042e6a5562... done.
The method returned:
Einstein (14/4/1879)

Gas consumption:
 * total: 6463
   * for CPU: 2474
   * for RAM: 3694
   * for storage: 295
   * for penalty: 0
 * price per unit: 1 pana
 * total price: 6463 panas
```

This time, the correct answer `Einstein (14/4/1879)` appears on the screen.

> In Ethereum, the only objects that can be passed, from outside the blockchain,
> as argument to method calls into blockchain are contracts. Namely, in Solidity
> it is possible to pass such objects as their untyped _address_ that can only
> be cast to contract classes. Takamaka allows more, since _any_ object can be passed as
> argument, not only contracts, as long as its class is annotated as `@Exported`.
> This includes all contracts since the class `io.takamaka.code.lang.Contract`, that
> we will present later, is annotated as `@Exported` and `@Exported` is an
> inherited Java annotation.

We can do the same in code, instead of using the `moka objects call` command. Namely, we can expand
the `FamilyStorage` class seen before in order to run a further transaction, that calls `toString`.
Copy then the following `FamilyExported` class inside the `io.hotmoka.tutorial.examples.runs`
package of the `io-hotmoka-tutorial-examples-runs` project:

```java
package io.hotmoka.tutorial.examples.runs;

import static io.hotmoka.helpers.Coin.panarea;
import static io.hotmoka.node.StorageTypes.INT;
import static java.math.BigInteger.ONE;

import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;

import io.hotmoka.constants.Constants;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.remote.RemoteNodes;

public class FamilyExported {

  private final static ClassType PERSON = StorageTypes.classNamed
    ("io.hotmoka.tutorial.examples.family.Person");

  public static void main(String[] args) throws Exception {

	// the path of the user jar to install
	var familyPath = Paths.get(System.getProperty("user.home")
      + "/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family/"
      + Constants.HOTMOKA_VERSION
      + "/io-hotmoka-tutorial-examples-family-" + Constants.HOTMOKA_VERSION + ".jar");

    var dir = Paths.get(args[1]);
    var payer = StorageValues.reference(args[2]);
    var password = args[3];

    try (var node = RemoteNodes.of(new URI(args[0]), 80000)) {
      // we get a reference to where io-takamaka-code-X.Y.Z.jar has been stored
      TransactionReference takamakaCode = node.getTakamakaCode();

      // we get the signing algorithm to use for requests
      var signature = node.getConfig().getSignatureForRequests();

	  KeyPair keys = loadKeys(node, dir, payer, password);

	  // we create a signer that signs with the private key of our account
	  Signer<SignedTransactionRequest<?>> signer = signature.getSigner
	    (keys.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);

	  // we get the nonce of our account: we use the account itself as caller and
	  // an arbitrary nonce (ZERO in the code) since we are running
	  // a @View method of the account
	  BigInteger nonce = node
	    .runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
	      (payer, // payer
	      BigInteger.valueOf(100_000), // gas limit
	      takamakaCode, // class path for the execution of the transaction
	      MethodSignatures.NONCE, // method
	      payer)).get() // receiver of the method call
	    .asBigInteger(__ -> new ClassCastException());

	  // we get the chain identifier of the network
	  String chainId = node.getConfig().getChainId();

	  var gasHelper = GasHelpers.of(node);

      // we install the family jar in the node: our account will pay
      TransactionReference family = node
        .addJarStoreTransaction(TransactionRequests.jarStore
          (signer, // an object that signs with the payer's private key
          payer, // payer
          nonce, // payer's nonce: relevant since this is not a call to a @View method!
          chainId, // chain identifier: relevant since this is not a call to a @View method!
          BigInteger.valueOf(300_000), // gas limit: enough for this very small jar
          gasHelper.getSafeGasPrice(), // gas price: at least the current gas price of the network
          takamakaCode, // class path for the execution of the transaction
          Files.readAllBytes(familyPath), // bytes of the jar to install
          takamakaCode)); // dependencies of the jar that is being installed

      // we increase our copy of the nonce, ready for further
      // transactions having the account as payer
      nonce = nonce.add(ONE);

      // call the constructor of Person and store in einstein the new object in blockchain
      StorageReference einstein = node.addConstructorCallTransaction
        (TransactionRequests.constructorCall
         (signer, // an object that signs with the payer's private key
         payer, // payer
         nonce, // payer's nonce: relevant since this is not a call to a @View method!
         chainId, // chain identifier: relevant since this is not a call to a @View method!
         BigInteger.valueOf(100_000), // gas limit: enough for a small object
         panarea(gasHelper.getSafeGasPrice()), // gas price, in panareas
         family, // class path for the execution of the transaction

         // constructor Person(String,int,int,int)
         ConstructorSignatures.of(PERSON, StorageTypes.STRING, INT, INT, INT),

         // actual arguments
         StorageValues.stringOf("Einstein"), StorageValues.intOf(14),
         StorageValues.intOf(4), StorageValues.intOf(1879)
      ));

      // we increase our copy of the nonce, ready for further
      // transactions having the account as payer
      nonce = nonce.add(ONE);

      StorageValue s = node.addInstanceMethodCallTransaction
        (TransactionRequests.instanceMethodCall
         (signer, // an object that signs with the payer's private key
          payer, // payer
          nonce, // payer's nonce: relevant since this is not a call to a @View method!
          chainId, // chain identifier: relevant since this is not a call to a @View method!
          BigInteger.valueOf(100_000), // gas limit: enough for a small object
          panarea(gasHelper.getSafeGasPrice()), // gas price, in panareas
          family, // class path for the execution of the transaction

          // method to call: String Person.toString()
          MethodSignatures.ofNonVoid(PERSON, "toString", StorageTypes.STRING),

          // receiver of the method to call
          einstein
        )).get();

        // we increase our copy of the nonce, ready for further
        // transactions having the account as payer
        nonce = nonce.add(ONE);

        // print the result of the call
        System.out.println(s);
    }
  }

  private static KeyPair loadKeys(Node node, Path dir, StorageReference account, String password)
		  throws Exception {

    return Accounts.of(account, dir)
    		.keys(password, SignatureHelpers.of(node).signatureAlgorithmFor(account));
  }
}
```

The interesting part is the call to `addInstanceMethodCallTransaction()` at the end of the previous listing.
It requires to resolve method `Person.toString()` using `einstein` as receiver
(the type `StorageTypes.STRING` is the return type of the method) and
to run the resolved method. It stores the result in
`s`, that subsequently prints on the standard output.

Run class `FamilyExported`. You will obtain the same result as with `moka objects call`:

```shell
$ mvn compile exec:java
    -Dexec.mainClass="io.hotmoka.tutorial.examples.runs.FamilyExported"
    -Dexec.args="ws://panarea.hotmoka.io:8001
        hotmoka_tutorial
        cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
        chocolate"
Einstein (14/4/1879)
```

As we have shown, method `addInstanceMethodCallTransaction()` can be used to
invoke an instance method on an object in the store of the node. This requires some
clarification. First of all, note that the signature of the method to
call is resolved and the resolved method is then invoked. If
such resolved method is not found (for instance, if we tried to call `tostring` instead
of `toString`), then `addInstanceMethodCallTransaction()` would end up in
a failed transaction. Moreover, the usual resolution mechanism of Java methods applies.
If, for instance, we invoked
`MethodSignatures.ofNonVoid(StorageTypes.OBJECT, "toString", StorageTypes.STRING)`
instead of
`MethodSignatures.ofNonVoid(PERSON, "toString", StorageTypes.STRING)`,
then method `toString` would have be resolved from the run-time class of
`einstein`, looking for the most specific implementation of `toString()`,
up to the `java.lang.Object` class, which would anyway end up in
running `Person.toString()`.

Method `addInstanceMethodCallTransaction()` can be used to invoke instance
methods with parameters. If a `toString(int)` method existed in `Person`,
then we could call it and pass 2019 as its argument, by writing:

```java
StorageValue s = node.addInstanceMethodCallTransaction
  (TransactionRequests.instanceMethodCall(
    ...

    // method to call: String Person.toString(int)
    MethodSignatures.ofNonVoid(PERSON, "toString", StorageTypes.STRING, StorageTypes.INT),

    // receiver of the method to call
    einstein,
  
    // actual argument(s)
    StorageValues.intOf(2019)
  )).get();
```

where we have added the formal parameter `StorageTypes.INT`
and the actual argument `StorageValues.intOf(2019)`. Note that method calls yields
an optional value in Hotmoka, since methods returning `void` actually return non value.
Consequently, we need the final call to `get()` above to get the actual
value returned by the method.

Method `addInstanceMethodCallTransaction()` cannot be used to call a static
method. For that, use `addStaticMethodCallTransaction()` instead, that accepts
a request similar to that for `addInstanceMethodCallTransaction()`, but without a receiver.

## Storage Types and Constraints on Storage Classes

We have seen how to invoke a constructor of a class to build an object in
the store of a node or to invoke a method on an object in the store of a node. Both constructors and
methods can receive arguments. Constructors yield a reference to a new
object, freshly allocated; methods might yield a returned value, if they are
not declared as `void`. This means that there is a bidirectional
exchange of data from outside the node to inside it, and back. But not any
kind of data can be exchanged:

1. The values that can be exchanged from inside the node to
   outside the node are called  _storage values_.
2. The values that can be exchanged from outside the node to
   inside the node are the same _storage values_ as above, with the extra constraint
   that objects must belong to an `@Exported` class.

The set of _storage values_ is the union of

1. primitive values of Java (characters, bytes, shorts, integers, longs, floats,
doubles and booleans);
2. reference values whose class extends `io.takamaka.code.lang.Storage` (that is, _storage objects_);
3. `null`;
4. a few special reference values: `java.math.BigInteger`s and `java.lang.String`s.

Storage values cross the
node's boundary inside wrapper objects. For instance the integer 2019
is first wrapped into `StorageValues.intOf(2019)` and then passed
as a parameter to a method or constructor. In our previous example,
when we called `Person.toString()`, the result `s` was actually a wrapper
of a `java.lang.String` object. Boxing and unboxing into/from wrapper objects
is automatic in Takamaka: our class `Person` does not show that machinery.

What should be retained of the above discussion is that constructors and
methods of Takamaka classes, if we want them to be called from outside the
node, must receive storage values as parameters and must return storage
values (if they are not `void` methods). A method that expects a parameter of
type `java.util.HashSet`, for instance, can be defined and called
from Takamaka code, inside the node, but cannot be called from outside the node,
such as, for instance, from the `moka` tool or from our `Family` class. The same
occurs if the method returns a `java.util.HashSet`.

We conclude this section with a formal definition of storage objects.
We have already said that storage objects can be kept in the store of a node
and that their class must extend
`io.takamaka.code.lang.Storage`. But there are extra constraints. Namely,
fields of a storage objects are part of the representation of such
objects and must, themselves, be kept in store. Hence, a storage object:

1. has a class that extends (directly or indirectly) `io.takamaka.code.lang.Storage`, and
2. is such that all its fields hold storage values (primitives, storage objects, `null`,
a `java.math.BigInteger` or a `java.lang.String`).

Note that the above conditions hold for the class `Person` defined above. Instead,
the following are examples of what is **not** allowed in a field of a storage object:

1. arrays
2. collections from `java.util.*`

We will see later how to overcome these limitations.

> Again, we stress that such limitations only apply to storage objects.
> Other objects, that needn't be kept in the store of a node but are useful for
> the implementation of Takamaka code, can be defined in a completely free way
> and used in code that runs in the node.

## Transactions Can Be Added, Posted and Run

We have executed transactions on a Hotmoka node with methods
`addJarStoreTransaction()`, `addConstructorCallTransaction()`
and `addInstanceMethodCallTransaction()`. These methods, whose name
starts with `add`,
are *synchronous*, meaning that they block until the transaction is
executed (or fails). If they are invoked on a node with a notion of
commit, such as a blockchain, they guarantee to block until
the transaction is actually committed.
In many cases, when we immediately need the result of a transaction
before continuing with the execution of the
subsequent statements,
these methods are the right choice. In many other cases, however,
it is unnecessary to wait until the transaction has completed
its execution and has been committed. In those cases, it can
be faster to execute a transaction through a method whose name
starts with `post`, such as
`postJarStoreTransaction()`, `postConstructorCallTransaction()`
or `postInstanceMethodCallTransaction()`. These methods are called
*asynchronous*, since they terminate
immediately, without waiting for the outcome of the transaction
they trigger. Hence they cannot return their outcome immediately
and return a *future*
instead, whose `get()` value, if and when invoked, will block
until the outcome of the transaction is finally available.

For instance, instead of the inefficient:

```java
StorageValue s = node.addInstanceMethodCallTransaction
  (TransactionRequests.instanceMethodCall(
    signer,
    payer,
    nonce,
    chainId,
    BigInteger.valueOf(100_000),
    panarea(gasHelper.getSafeGasPrice()),
    family,
    MethodSignatures.ofNonVoid(PERSON, "toString", StorageTypes.STRING),
    einstein
  )).get();

// code that does not use s
// .....
```

one can write the more efficient:

```java
MethodFuture future = node.postInstanceMethodCallTransaction
  (TransactionRequests.instanceMethodCall(
    signer,
    payer,
    nonce,
    chainId,
    BigInteger.valueOf(100_000),
    panarea(gasHelper.getSafeGasPrice()),
    family,
    MethodSignatures.ofNonVoid(PERSON, "toString", StorageTypes.STRING),
    einstein
  ));

// code that does not use s
// .....

// the following will be needed only if s is used later
StorageValue s = future.get().get();
```

where the first `get()` belongs to the future and is a blocking call
that suspends until the method has been executed, and the second
`get()` extracts the value from the optional returned by Hotmoka's
API for calling methods.

There is a third way to execute a transaction. Namely, calls to methods
annotated as `@View` can be performed through the
`runInstanceMethodCallTransaction()` (for instance methods) and
`runStaticMethodCallTransaction()` (for static methods).
As we have hinted before, these executions are performed
locally, on the node they are addressed to, and do not add a transaction
that must be replicated in each node of the network, for consensus, and
that costs gas for storage.
These executions are free and do not require a correct nonce, signature,
or chain identifier, which is a great simplification.

# The Notion of Smart Contract

A contract is a legal agreement among two or more parties. A good contract
should be unambiguous, since otherwise its interpretation could be
questioned or misunderstood. A legal system normally enforces the
validity of a contract. In the context of software development, a *smart contract*
is a piece of software with deterministic behavior, whose semantics should be
clear and enforced by a consensus system. Blockchains provide the perfect
environment where smart contracts can be deployed and executed, since their
(typically) non-centralized nature reduces the risk that a single party
overthrows the rules of consensus, by providing for instance a non-standard
semantics for the code of the smart contract.

Contracts are allowed to hold and transfer money to other contracts. Hence,
traditionally, smart contracts are divided into those that hold money
but have no code (*externally owned accounts*), and those that,
instead, contain code (*smart contracts*).
The formers are typically controlled by an external agent (a wallet,
a human or a software application, on his behalf)
while the latters are typically controlled by their code.
Takamaka implements both alternatives as instances of the abstract library class
`io.takamaka.code.lang.Contract` (inside `io-takamaka-code`). That class extends
`io.takamaka.code.lang.Storage`, hence its instances can be kept in the store
of the node. Moreover, that class is annotated as `@Exported`, hence nodes can receive
references to contract instances from the outside world.
The Takamaka library defines subclasses of `io.takamaka.code.lang.Contract`, that
we will investigate later. Programmers can define their own subclasses as well.

This chapter presents a simple smart contract, whose goal is to
enforce a Ponzi investment scheme: each investor pays back the previous investor,
with at least a 10% reward; as long as new
investors keep coming, each investor gets at least a 10% reward; the last
investor, instead, will never see his/her investment back.
The contract has been inspired by a similar Ethereum contract, shown
at page 145 of [[IyerD08]](#references).

We will develop the contract in successive versions, in order to highlight
the meaning of different language features of Takamaka.

## A Simple Ponzi Scheme Contract

__[See `io-hotmoka-tutorial-examples-ponzi_simple` in `https://github.com/Hotmoka/hotmoka`]__

Create a new Maven Java 21 (or later) project in Eclipse, named `io-hotmoka-tutorial-examples-ponzi`.
You can do this by duplicating the project `io-hotmoka-tutorial-examples-family`. Use the following `pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                        http://maven.apache.org/xsd/maven-4.0.0.xsd">

  <modelVersion>4.0.0</modelVersion>
  <groupId>io.hotmoka</groupId>
  <artifactId>io-hotmoka-tutorial-examples-ponzi</artifactId>
  <version>1.9.0</version>

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.release>21</maven.compiler.release>
  </properties>

  <dependencies>
    <dependency>
      <groupId>io.hotmoka</groupId>
      <artifactId>io-takamaka-code</artifactId>
      <version>1.5.0</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.11.0</version>
      </plugin>
    </plugins>
  </build>

</project>
```

and the following `module-info.java`:

```java
module ponzi {
  requires io.takamaka.code;
}
```

Create package `io.hotmoka-tutorial.examples.ponzi` inside `src/main/java` and add
the following `SimplePonzi.java` source inside that package:

```java
package io.hotmoka.tutorial.examples.ponzi;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.StringSupport;
import io.takamaka.code.math.BigIntegerSupport;

public class SimplePonzi extends Contract {
  private final BigInteger _10 = BigInteger.valueOf(10L);
  private final BigInteger _11 = BigInteger.valueOf(11L);
  private Contract currentInvestor;
  private BigInteger currentInvestment = BigInteger.ZERO;

  public void invest(Contract investor, BigInteger amount) {
    // new investments must be at least 10% greater than current
    BigInteger minimumInvestment = BigIntegerSupport.divide
      (BigIntegerSupport.multiply(currentInvestment, _11), _10);
    require(BigIntegerSupport.compareTo(amount, minimumInvestment) > 0,
      () -> StringSupport.concat("you must invest more than ", minimumInvestment));

    // document new investor
    currentInvestor = investor;
    currentInvestment = amount;
  }
}
```

> This code is only the starting point of our discussion and is not functional yet.
> The real final version of this contract will appear at the end of this section.

Look at the code of `SimplePonzi.java` above. The contract has a single
method, named `invest`. This method lets a new `investor` invest
a given `amount` of coins. This amount must be at least 10% higher than
the current investment. The expression `BigIntegerSupport.compareTo(amount, minimumInvestment) > 0`
is a comparison between two Java `BigInteger`s and should be read as the
more familiar `amount >= minimumInvestment`: the latter cannot be
written in this form, since Java does not allow comparison operators
to work on reference types.
The static method `io.takamaka.code.lang.Takamaka.require()` is used to require
some precondition to hold. The `require(condition, message)` call throws an
exception if `condition` does not hold, with the given `message`.
If the new investment is at least 10% higher than the current one, it will be
saved in the state of the contract, together with the new investor.

> You might wonder why we have written
> `require(..., () -> StringSupport.concat("you must invest more than ", minimumInvestment)`
> instead of the simpler
> `require(..., StringSupport.concat("you must invest more than ", minimumInvestment)`.
> Both are possible and semantically almost identical. However, the former
> uses a lambda expression that computes the string concatenation lazily, only if
> the message is needed; the latter always computes the string concatenation, instead.
> Hence, the first version consumes less gas, in general, and is consequently
> preferred. This technique simulates lazy evaluation in a language, like
> Java, that has only eager evaluation for actual arguments. This technique
> has been used since years, for instance in JUnit assertions.

## The `@FromContract` and `@Payable` Annotations

__[See `io-hotmoka-tutorial-examples-ponzi_annotations` in `https://github.com/Hotmoka/hotmoka`]__

The previous code of `SimplePonzi.java` is unsatisfactory, for at least two
reasons, that we will overcome in this section:

1. Any contract can call `invest()` and let _another_ `investor` contract invest
   in the game. This is against our intuition that each investor decides when
   and how much he (himself) decides to invest.
2. There is no money transfer. Anybody can call `invest()`, with an arbitrary
   `amount` of coins. The previous investor does not get the investment back
   when a new investor arrives since, well, he never really invested anything.

Let us rewrite `SimplePonzi.java` in the following way:

```java
package io.hotmoka.tutorial.examples.ponzi;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.StringSupport;
import io.takamaka.code.math.BigIntegerSupport;

public class SimplePonzi extends Contract {
  private final BigInteger _10 = BigInteger.valueOf(10L);
  private final BigInteger _11 = BigInteger.valueOf(11L);
  private Contract currentInvestor;
  private BigInteger currentInvestment = BigInteger.ZERO;

  public @FromContract void invest(BigInteger amount) {
	// new investments must be at least 10% greater than current
    BigInteger minimumInvestment = BigIntegerSupport.divide
      (BigIntegerSupport.multiply(currentInvestment, _11), _10);
    require(BigIntegerSupport.compareTo(amount, minimumInvestment) > 0,
      () -> StringSupport.concat("you must invest more than ", minimumInvestment));

    // document new investor
    currentInvestor = caller();
    currentInvestment = amount;
  }
}
```

The difference with the previous version of `SimplePonzi.java`
is that the `investor` argument of `invest()` has disappeared.
At its place, `invest()` has been annotated as `@FromContract`. This annotation
**restricts** the possible uses of method `invest()`. Namely, it can
only be called from a contract object *c* or from an external wallet,
with a paying contract *c*, that pays for a transaction that runs
`invest()`. It cannot, instead, be called from
the code of a class that is not a contract.
The instance of contract *c* is available, inside
`invest()`, as `caller()`. This is, indeed, saved, in the above code,
into `currentInvestor`.

The annotation `@FromContract` can be applied to both methods and constructors.
If a `@FromContract` method is redefined, the redefinitions must also be
annotated as `@FromContract`.

> Method `caller()` can only be used inside a `@FromContract` method or
> constructor and refers to the contract that called that method or constructor
> or to the contract that pays for a call, from a wallet, to the method or constructor.
> Hence, it will never yield `null`. If a `@FromContract` method or constructor
> calls another method *m*, then the `caller()` of the former is **not** available
> inside *m*, unless the call occurs, syntactically, on `this`, in which case
> the `caller()` is preserved. By _syntactically_, we mean through expressions such as
> `this.m(...)` or `super.m(...)`.

The use of `@FromContract` solves the first problem: if a contract invests in the game,
then it is the caller of `invest()`. However, there is still no money
transfer in this version of `SimplePonzi.java`. What we still miss is to require
the caller of `invest()` to actually pay for the `amount` units of coin.
Since `@FromContract` guarantees that the caller of `invest()` is a contract and since
contracts hold money, this means that the caller contract of `invest()`
can be charged `amount` coins at the moment of calling `invest()`.
This can be achieved with the `@Payable` annotation, that we apply to `invest()`:

```java
package io.hotmoka.tutorial.examples.ponzi;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.StringSupport;
import io.takamaka.code.math.BigIntegerSupport;

public class SimplePonzi extends Contract {
  private final BigInteger _10 = BigInteger.valueOf(10L);
  private final BigInteger _11 = BigInteger.valueOf(11L);
  private Contract currentInvestor;
  private BigInteger currentInvestment = BigInteger.ZERO;

  public @Payable @FromContract void invest(BigInteger amount) {
	// new investments must be at least 10% greater than current
    BigInteger minimumInvestment = BigIntegerSupport.divide
      (BigIntegerSupport.multiply(currentInvestment, _11), _10);
    require(BigIntegerSupport.compareTo(amount, minimumInvestment) > 0,
      () -> StringSupport.concat("you must invest more than ", minimumInvestment));

    // document new investor
    currentInvestor = caller();
    currentInvestment = amount;
  }
}
```

When a contract calls `invest()` now, that contract will be charged `amount` coins,
automatically. This means that these coins will be automatically transferred to the
balance of the instance of `SimplePonzi` that receives the call.
If the balance of the calling contract is too little for that, the call
will be automatically rejected with an insufficient funds exception. The caller
must be able to pay for both `amount` and the gas needed to run `invest()`. Hence,
he must hold a bit more than `amount` coins at the moment of calling `invest()`.

> The `@Payable` annotation can only be applied to a method or constructor that
> is also annotated as `@FromContract`. If a `@Payable` method is redefined, the redefinitions
> must also be annotated as `@Payable`. A `@Payable` method or constructor
> must have a first argument of type `int`, `long` or `java.math.BigInteger`,
> depending on the amount of coins that the programmer allows one to transfer
> at call time. The name of that argument is irrelevant, but we will keep
> using `amount` for it.

## Payable Contracts

__[See `io-hotmoka-tutorial-examples-ponzi_payable` in `https://github.com/Hotmoka/hotmoka`]__

The `SimplePonzi.java` class is not ready yet. Namely, the code
of that class specifies that investors have to pay
an always increasing amount of money to replace the current investor.
However, in the current version of the code,
the replaced investor never gets his previous investment back, plus the 10% award
(at least): money keeps flowing inside the `SimplePonzi` contract and remains
stuck there, forever. The code needs an apparently simple change: just add a single statement
before the update of the new current investor. That statement should send
`amount` units of coin back to `currentInvestor`, before it gets replaced:

```java
// document new investor
if (currentInvestor != null)
  currentInvestor.receive(amount);
currentInvestor = caller();
currentInvestment = amount;
```

In other words, a new investor calls `invest()` and pays `amount` coins to
the `SimplePonzi` contract (since `invest()` is `@Payable`); then
this `SimplePonzi` contract transfers the same `amount` of coins to pay back the
previous investor. Money flows through the `SimplePonzi` contract but
does not stay there for long.

The problem with this simple line of code is that it does not compile.
There is no `receive()` method in `io.takamaka.code.lang.Contract`:
a contract can receive money only through calls to its `@Payable`
constructors and methods. Since `currentInvestor` is, very generically,
an instance of `Contract`, that has no `@Payable` methods,
there is no method
that we can call here for sending money back to `currentInvestor`.
This limitation is a deliberate design choice of Takamaka.

> Solidity programmers will find this very different from what happens
> in Solidity contracts. Namely, these always have a _fallback function_ that
> can be called for sending money to a contract. A problem with Solidity's approach
> is that the balance of a contract is not fully controlled by its
> payable methods, since money can always flow in through the fallback
> function (and also in other, more surprising ways).
> This led to software bugs, when a contract found itself
> richer then expected, which violated some (wrong) invariants about
> its state. For more information, see page 181 of
> [[AntonopoulosW19]](#references) (*Unexpected Ether*).

So how do we send money back to `currentInvestor`? The solution is to
restrict the kind of contracts that can participate to the Ponzi scheme.
Namely, we limit the game to contracts that implement class
`io.takamaka.code.lang.PayableContract`, a subclass of `io.takamaka.code.lang.Contract`
that, yes, does have a payable `receive()` method. This is not really a restriction,
since the typical players of our Ponzi contract are externally
owned accounts, that are instances of `PayableContract`.

Let us hence apply the following small changes to our `SimplePonzi.java` class:

1. The type of `currentInvestment` must be restricted to `PayableContract`.
2. The `invest()` method must be callable by `PayableContract`s only.
3. The return value of `caller()` must be cast to `PayableContract`, which is
   safe because of point 2 above.

The result is the following:

```java
package io.hotmoka.tutorial.examples.ponzi;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.StringSupport;
import io.takamaka.code.math.BigIntegerSupport;

public class SimplePonzi extends Contract {
  private final BigInteger _10 = BigInteger.valueOf(10L);
  private final BigInteger _11 = BigInteger.valueOf(11L);
  private PayableContract currentInvestor;
  private BigInteger currentInvestment = BigInteger.ZERO;

  public @Payable @FromContract(PayableContract.class) void invest(BigInteger amount) {
    // new investments must be at least 10% greater than current
    BigInteger minimumInvestment = BigIntegerSupport.divide
      (BigIntegerSupport.multiply(currentInvestment, _11), _10);
    require(BigIntegerSupport.compareTo(amount, minimumInvestment) > 0,
      () -> StringSupport.concat("you must invest more than ", minimumInvestment));

    // document new investor
    if (currentInvestor != null)
    	currentInvestor.receive(amount);

    currentInvestor = (PayableContract) caller();
    currentInvestment = amount;
  }
}
```

Note the use of `@FromContract(PayableContract.class)` in the code above:
a method or constructor
annotated as `@FromContract(C.class)` can only be called by a contract whose class
is `C` or a subclass of `C`. Otherwise, a run-time exception will occur.

## The `@View` Annotation

__[See `io-hotmoka-tutorial-examples-ponzi_view` in `https://github.com/Hotmoka/hotmoka`]__

Our `SimplePonzi.java` code can still be improved. As it is now,
an investor must call `invest()` and be ready to pay a sufficiently
large `amount` of coins to pay back and replace the previous investor.
How much is *large* actually large enough? Well, it depends on the
current investment. But that information is kept inside the contract
and there is no easy way to access it from outside.
An investor can only try with something that looks large enough,
running a transaction that might end up in two scenarios,
both undesirable:

1. The amount invested was actually large enough, but larger than needed: the investor
   invested more than required in the Ponzi scheme, taking the risk that no one
   will ever invest more and pay him back.
2. The amount invested might not be enough: the `require()` function
   will throw an exception that makes the transaction running `invest()` fail.
   The investment will not be transferred to the `SimplePonzi` contract, but
   the investor will be penalized by charging him all the gas provided for
   the transaction. This is unfair since, after all, the investor had no
   way to know that the proposed investment was not large enough.

Hence, it would be nice and fair to provide investors with a way to access
the value in the `currentInvestment` field.
This is actually a piece of cake: just add
this method to `SimplePonzi.java`:

```java
public BigInteger getCurrentInvestment() {
  return currentInvestment;
}
```

This solution is perfectly fine but can be improved. Written this way,
an investor that wants to call `getCurrentInvestment()` must run a
Hotmoka transaction through the `addInstanceMethodCallTransaction()`
method of the node, creating a new transaction that ends up in
the store of the node. That transaction will cost gas, hence its side-effect will
be to reduce the balance of the calling investor. But the goal of the caller
was just to access information in the store of the node, not to modify the store through
side-effects. The balance reduction for the caller is, indeed, the *only*
side-effect of that call! In cases like this, Takamaka allows one to
specify that a method is expected to have no side-effects on the visible
state of the node, but for the change of the balance of the caller.
This is possible through the `@View` annotation. Import that
class in the Java source and edit the declaration of `getCurrentInvestment()`,
as follows:

```java
import io.takamaka.code.lang.View;
...
  public @View BigInteger getCurrentInvestment() {
    return currentInvestment;
  }
```

An investor can now call that method through another API method of the Hotmoka
nodes, called `runInstanceMethodCallTransaction()`, that does not expand the
store of the node, but yields the response of the transaction, including the
returned value of the call. If method
`getCurrentInvestment()` had side-effects beyond that on the balance of
the caller, then the execution will fail with a run-time exception.
Note that the execution of a `@View` method still requires gas,
but that gas is given back at the end of the call.
The advantage of `@View` is hence that of allowing the execution
of `getCurrentInvestment()` for free and without expanding the store of the node
with useless transactions, that do not modify its state. Moreover,
transactions run through `runInstanceMethodCallTransaction()` do not need
a correct nonce, chain identifier or signature,
hence any constant value can be used for them.
This simplifies the call. For the same reason, transactions run
through `runInstanceMethodCallTransaction()` do not count for the computation of
the nonce of the caller.

> The annotation `@View` is checked at run time if a transaction calls the
> `@View` method from outside the blockchain, directly. It is not checked if,
> instead, the method is called indirectly, from other Takamaka code.
> The check occurs at run time, since the presence of side-effects in
> computer code is undecidable. Future versions of Takamaka might check
> `@View` at the time of installing a jar in a node, as part of
> bytecode verification. That check can only be an approximation of the
> run-time check.

> If a `@View` method is called through the `moka objects call` command,
> the `moka` tool will automatically perform a `runInstanceMethodCallTransaction()`
> internally, to spare gas, and the call will occur for free.

## The Hierarchy of Contracts

Figure 23 shows the hierarchy of Takamaka contract classes.
The topmost abstract class `io.takamaka.code.lang.Contract`
extends `io.takamaka.code.lang.Storage`, since contracts are meant to be
stored in a node (as are other classes that are not contracts,
such as our first `Person` example).
Programmers typically extend `Contract` to define their own contracts.
This is the case, for instance, of our `SimplePonzi` class.
Class `Storage` provides a `caller()` final protected method that can be called inside
`@FromContract` methods and constructors, to access the calling contract.
Class `Contract` provides a final `@View` method `balance()` that
can be used to access the private `balance` field of the contract.
Note that class `Contract` is annotated with the inherited annotation `@Exported`,
hence contracts, such as instances of `SimplePonzi`, can be receivers of calls
from outside the node and can be passed as arguments to calls from outside the node.
Instances of `Storage` are not normally `@Exported`, unless their class
is explicitly annotated as `@Exported`, as we did for `Person`.

 <p align="center"><img width="700" src="pics/contracts.png" alt="Figure 23. The hierarchy of contract classes"></p><p align="center">Figure 23. The hierarchy of contract classes.</p>


The abstract subclass `PayableContract` is meant for contracts that
can receive coins from other contracts, through their final
`receive()` methods. Its concrete subclass named `ExternallyOwnedAccount` is
a payable contract that can be used to pay for a transaction.
Such _accounts_ are typically controlled by humans, through a wallet, but can be
subclassed and instantiated freely in Takamaka code. Their constructors
allow one to build an externally owned account and fund it with an initial
amount of coins. As we have seen in sections
[Installation of the Jar in a Hotmoka Node](#installation-of-the-jar-in-a-hotmoka-node),
[Creation of an Object of our Program](#creation-of-an-object-of-our-program) and
[Calling a Method on an Object in a Hotmoka Node](#calling-a-method-on-an-object-in-a-hotmoka-node),
the methods of Hotmoka nodes that start a transaction require to specify a payer
for that transaction. Such a payer is required to be an instance of
`ExternallyOwnedAccount`, or an exception will be thrown. In our previous examples,
we have used, as payer, an account created by the `moka accounts create` command,
that is an instance of `io.takamaka.code.lang.ExternallyOwnedAccount`.
`ExternallyOwnedAccount`s have a private field `nonce` that can be accessed through
the public `@View` method `nonce()`: it yields a `BigInteger`
that specifies the next nonce to use for the next transaction having that
account as caller. This nonce gets automatically increased after each such transaction.

Instances of `ExternallyOwnedAccount`s hold their public key in their
private `publicKey` field, as a Base64-encoded string,
that can be accessed through the `publicKey()` method.
That key is used to verify the signature of the transactions
having that account as caller. As we will see later, there is a default signature
algorithm for transactions and that is what `ExternallyOwnedAccount`s use.
However, it is possible to require a specific signature algorithm, that overrides the default
for the node. For that, it is enough to instantiate classes `ExternallyOwnedAccountSHA256DSA`,
`ExternallyOwnedAccountED25519`, `ExternallyOwnedAccountQTESLA1`
or `ExternallyOwnedAccountQTESLA3`. The latter two use a
quantum-resistant signature algorithm
(see [Signatures and Quantum-Resistance](#signatures-and-quantum-resistance)
for more details). This means that it is possible
to mix many signature algorithms for signing transactions inside the same Hotmoka node,
as we will show later.

# The Support Library

This chapter presents the support library of the Takamaka language,
that contains classes for simplifying the definition of smart contracts.

In [Storage Types and Constraints on Storage Classes](#storage-types-and-constraints-on-storage-classes),
we said that storage objects must obey to some constraints.
The strongest of them is that their fields of reference type, in turn, can only hold
storage objects. In particular, arrays are not allowed there. This can
be problematic, in particular for contracts that deal with a
dynamic, variable, potentially unbound number of other contracts.

Therefore, most classes of the support library deal
with such constraints, by providing fixed or variable-sized collections
that can be used in storage objects, since they are storage objects themselves.
Such utility classes implement lists, arrays and maps and are
consequently generally described as *collections*. They have the
property of being storage classes, hence their instances can be kept in
the store of a Hotmoka node, *as long as only storage objects are added as elements of
the collection*. As usual with collections, these utility classes
have generic type, to implement collections of arbitrary, but fixed
types. This is not problematic, since Java (and hence Takamaka) allows generic types.

## Storage Lists

Lists are an ordered sequence of elements. In a list, it is typically
possible to access the first element in constant time, while accesses
to the *n*th element require to scan the list from its head and
consequently have a cost proportional to *n*. Because of this,
lists are **not**, in general, random-access data structures, whose *n*th
element should be accessible in constant time. It is also possible
to add an element at the beginning of a list, in constant time.
The size of a list is not fixed: lists grow in size as more elements are added.

Java has many classes for implementing lists, all subclasses
of `java.util.List<E>`. They cannot be used in Takamaka that, instead,
provides an implementation of lists with the storage class
`io.takamaka.code.util.StorageLinkedList<E>`. Its instances are storage objects and
can consequently be held in fields of storage classes and
can be stored in a Hotmoka node, *as long as only
storage objects are added to the list*. Takamaka lists provide
constant-time access and addition to both ends of a list.
We refer to the JavaDoc of `StorageLinkedList<E>` for a full description of its methods.
They include methods for adding elements to either ends of the list, for accessing and
removing elements, for iterating on a list and for building a Java array
`E[]` holding the elements of a list.

 <p align="center"><img width="450" src="pics/lists.png" alt="Figure 24. The hierarchy of storage lists"></p><p align="center">Figure 24. The hierarchy of storage lists.</p>


Figure 24 shows the hierarchy of the `StorageLinkedList<E>` class.
It implements the interface `StorageList<E>`, that defines the methods that modify a list.
That interface extends the interface `StorageListView<E>` that, instead, defines the methods
that read data from a list, but do not modify it. This distinction between the _read-only_
interface and the _modification_ interface is typical of all collection classes in the
Takamaka library, as we will see. For the moment, note that this distinction is useful
for defining methods `snapshot()` and `view()`. Both return a `StorageListView<E>` but there
is an important difference between them. Namely, `snapshot()` yields a _frozen_ view of the list,
that cannot and will never be modified, also if the original list gets subsequently updated. Instead,
`view()` yields a _view_ of a list, that is, a read-only list that changes whenever
the original list changes and exactly in the same way: if an element is added to the original
list, the same automatically occurs to the view.
In this sense, a view is just a read-only alias of the original list.
Both methods can be useful to export data, safely,
from a node to the outside world, since both methods
return an `@Exported` object without modification methods.
Method `snapshot()` runs in linear time (in the length of the list)
while method `view()` runs in constant time.

> It might seem that `view()` is just an upwards cast to the
> interface `StorageListView<E>`. This is wrong, since that method
> does much more. Namely, it applies the façade design pattern
> to provide a _distinct_ list that lacks any modification method
> and implements a façade of the original list.
> To appreciate the difference to a cast, assume to have a `StorageList<E> list` and to write
> `StorageListView<E> view = (StorageListView<E>) list`. This upwards cast will always succeed.
> Variable `view` does not allow to call any modification method, since they
> are not in its static type `StorageListView<E>`. But a downwards cast back to `StorageList<E>`
> is enough to circumvent
> that constraint: `StorageList<E> list2 = (StorageList<E>) view`. This way, the original `list`
> can be modified by modifying `list2` and it would not be safe to export `view`, since it
> is a Trojan horse for the modification of `list`. With method `view()`, the
> problem does not arise, since the cast `StorageList<E> list2 = (StorageList<E>) list.view()`
> fails: method `view()` actually returns another list object without modification methods.
> The same is true for method `snapshot()` that, moreover, yields a frozen view of the
> original list. These same considerations hold for the other Takamaka collections that we will
> see in this chapter.

Next section shows an example of use for `StorageLinkedList`.

### A Gradual Ponzi Contract

__[See `io-hotmoka-tutorial-examples-ponzi_gradual` in `https://github.com/Hotmoka/hotmoka`]__

Consider our previous Ponzi contract again. It is somehow irrealistic, since
an investor gets its investment back in full. In a more realistic scenario,
the investor will receive the investment back gradually, as soon as new
investors arrive. This is more complex to program, since
the Ponzi contract must take note of all investors that invested up to now,
not just of the current one as in `SimplePonzi.java`. This requires a
list of investors, of unbounded size. An implementation of this gradual
Ponzi contract is reported below and has been
inspired by a similar Ethereum contract from Iyer and Dannen,
shown at page 150 of [[IyerD08]](#references).
Write its code inside package `io.hotmoka.tutorial.examples.ponzi` of
the `io-hotmoka-tutorial-examples-ponzi` project, as a new class `GradualPonzi.java`:

```java
package io.hotmoka.tutorial.examples.ponzi;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.StringSupport;
import io.takamaka.code.math.BigIntegerSupport;
import io.takamaka.code.util.StorageLinkedList;
import io.takamaka.code.util.StorageList;

public class GradualPonzi extends Contract {
  public final BigInteger MINIMUM_INVESTMENT = BigInteger.valueOf(1_000L);

  /**
   * All investors up to now. This list might contain the same investor many times,
   * which is important to pay him back more than investors who only invested once.
   */
  private final StorageList<PayableContract> investors = new StorageLinkedList<>();

  public @FromContract(PayableContract.class) GradualPonzi() {
    investors.add((PayableContract) caller());
  }

  public @Payable @FromContract(PayableContract.class) void invest(BigInteger amount) {
	 // new investments must be at least 10% greater than current
    require(BigIntegerSupport.compareTo(amount, MINIMUM_INVESTMENT) >= 0,
      () -> StringSupport.concat("you must invest at least ", MINIMUM_INVESTMENT));
    BigInteger eachInvestorGets = BigIntegerSupport.divide
      (amount, BigInteger.valueOf(investors.size()));
    investors.forEach(investor -> investor.receive(eachInvestorGets));
    investors.add((PayableContract) caller());
  }
}
```

The constructor of `GradualPonzi` is annotated as `@FromContract`, hence
it can only be
called by a contract, that gets added, as first investor,
in the `io.takamaka.code.util.StorageLinkedList` held in field `investors`.
This list, that implements an unbounded list of objects,
is a storage object, as long as only storage objects are
added inside it. `PayableContract`s are storage objects, hence
its use is correct here.
Subsequently, other contracts can invest by calling method `invest()`.
A minimum investment is required, but this remains constant over time.
The `amount` invested gets split by the number of the previous investors
and sent back to each of them. Note that Takamaka allows programmers to use
Java 8 lambdas.
Old fashioned Java programmers, who don't feel at home with such treats,
can exploit the fact that
storage lists are iterable and replace the single-line `forEach()` call
with a more traditional (but gas-hungrier):

```java
for (PayableContract investor: investors)
  investor.receive(eachInvestorGets);
```

It is instead **highly discouraged** to iterate the list as if it were an
array. Namely, **do not write**

```java
for (int pos = 0; pos < investors.size(); pos++)
  investors.get(i).receive(eachInvestorGets);
```

since linked lists are not random-access data structures and the complexity of the
last loop is quadratic in the size of the list. This is not a novelty: the
same occurs with many traditional Java lists, that do not implement
`java.util.RandomAccess` (a notable example is `java.util.LinkedList`).
In Takamaka, code execution costs gas and
computational complexity does matter, more than in other programming contexts.

### A Note on Re-entrancy

The `GradualPonzi.java` class pays back previous investors immediately:
as soon as a new investor invests something, his investment gets
split and forwarded to all previous investors. This should
make Solidity programmers uncomfortable, since the same approach,
in Solidity, might lead to the infamous re-entrancy attack, when the
contract that receives his investment back has a
fallback function redefined in such a way to re-enter the paying contract and
re-execute the distribution of the investment.
As it is well known, such an attack has made some people rich and other
desperate. You can find more detail
at page 173 of [[AntonopoulosW19]](#references).
Even if such a frightening scenario does not occur,
paying back previous investors immediately is discouraged in Solidity
also for other reasons. Namely, the contract that receives his
investment back might have a redefined fallback function that
consumes too much gas or does not terminate. This would hang the
loop that pays back previous investors, actually locking the
money inside the `GradualPonzi` contract. Moreover, paying back
a contract is a relatively expensive operation in Solidity, even if the
fallback function is not redefined, and this cost is paid by the
new investor that called `invest()`, in terms of gas. The cost is linear
in the number of investors that must be paid back.

As a solution to these problems, Solidity programmers do not pay previous
investors back immediately, but let the `GradualPonzi` contract take
note of the balance of each investor, through a map.
This map is updated as soon as a new investor arrives, by increasing the
balance of every previous investor. The cost of updating the balances
is still linear in the number of previous investors, but it is cheaper
(in Solidity) than sending money back to each of them, which
requires expensive inter-contract calls that trigger new sub-transactions.
With this technique, previous investors are
now required to withdraw their balance explicitly and voluntarily,
through a call to some function, typically called `widthdraw()`.
This leads to the *withdrawal pattern*, widely used for writing Solidity contracts.

We have not used the withdrawal pattern in `GradualPonzi.java`. In general,
there is no need for such pattern in Takamaka, at least not for simple
contracts like `GradualPonzi.java`. The reason is that the
`receive()` methods of a payable contract (corresponding to the
fallback function of Solidity) are `final` in Takamaka and very cheap
in terms of gas. In particular, inter-contract calls are not
especially expensive in Takamaka, since they are just a method
invocation in Java bytecode (one bytecode instruction). They are *not* inner transactions.
They are actually cheaper than
updating a map of balances. Moreover, avoiding the `withdraw()` transactions
reduces the overall number of transactions;
without using the map supporting the withdrawal pattern, Takamaka contracts
consume less gas and less storage.
Hence, the withdrawal pattern is both
useless in Takamaka and more expensive than paying back previous contracts immediately.

### Running the Gradual Ponzi Contract

Let us play with the `GradualPonzi` contract now.
We can now start by installing that jar in the node:

```shell
$ cd hotmoka_tutorial/io-hotmoka-tutorial-examples-ponzi   # if not already there
$ mvn install
$ cd ..
$ moka jars install cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    io-hotmoka-tutorial-examples-ponzi/target/io-hotmoka-tutorial-examples-ponzi-1.9.0.jar
    --password-of-payer
    --uri ws://panarea.hotmoka.io:8001

Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate
Do you really want to install the jar spending up to 953600 gas units
  at the price of 1 pana per unit (that is, up to 953600 panas) [Y/N] Y
Adding transaction 7a81dd5e19708146ba81fe639a048f3b0f56aed4362fc473fada31e487bd142a... done.
The jar has been installed at 7a81dd5e19708146ba81fe639a048f3b0f56aed4362fc473fada31e487bd142a.

Gas consumption:
 * total: 11283
   * for CPU: 1628
   * for RAM: 3351
   * for storage: 6304
   * for penalty: 0
 * price per unit: 1 pana
 * total price: 11283 panas
```

Create two more accounts now, letting our first account pay:

```shell
$ moka keys create --name=account2.pem --password
Enter value for --password
  (the password that will be needed later to use the key pair): orange
$ moka keys create --name=account3.pem --password
Enter value for --password
  (the password that will be needed later to use the key pair): apple
...
$ moka accounts create cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    50000000000 account2.pem --password --password-of-payer
    --uri=ws://panarea.hotmoka.io:8001
Enter value for --password (the password of the key pair): orange 
Enter value for --password-of-payer (the password of the payer): chocolate 
Do you really want to create the new account spending up to 200000 gas units
  at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y
Adding transaction f46e68a534589213d5df03af52498880aeaee3d25b8243ec12395993f57844d3... done.
A new account f46e68a534589213d5df03af52498880aeaee3d25b8243ec12395993f57844d3#0 has been created.
...
$ moka accounts create cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    10000000 account3.pem --password --password-of-payer
    --uri=ws://panarea.hotmoka.io:8001
Enter value for --password (the password of the key pair): apple 
Enter value for --password-of-payer (the password of the payer): chocolate 
Do you really want to create the new account spending up to 200000 gas units
  at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y
Adding transaction f544a0fa918fa2102bf59a9106e03c9c51f1d7d348930bd9e0b680aaef5c98ed... done.
A new account f544a0fa918fa2102bf59a9106e03c9c51f1d7d348930bd9e0b680aaef5c98ed#0 has been created.
...
```

We let our first account create an instance of `GradualPonzi` in the node now
and become the first investor of the contract:

```shell
$ moka objects create cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    io.hotmoka.tutorial.examples.ponzi.GradualPonzi
    --classpath=7a81dd5e19708146ba81fe639a048f3b0f56aed4362fc473fada31e487bd142a
    --password-of-payer
    --uri=ws://panarea.hotmoka.io:8001
Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate
Do you really want to call constructor public ...GradualPonzi() spending up to 200000 gas units
  at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y
Adding transaction e13885edde4ae93b1b66c906980d069f65cbc7237f7eca59b85759d5b8921ae8... done.
A new object 38dfb6f1dbe91cc4c6f5f501f8fd6c90aba36cc3150bd726a791fee315a27bae#0 has been created.
```

We let the other two players invest, in sequence, in the `GradualPonzi` contract:

```shell
$ moka objects call f46e68a534589213d5df03af52498880aeaee3d25b8243ec12395993f57844d3#0
    io.hotmoka.tutorial.examples.ponzi.GradualPonzi
    invest
    5000
    --classpath=7a81dd5e19708146ba81fe639a048f3b0f56aed4362fc473fada31e487bd142a
    --receiver=38dfb6f1dbe91cc4c6f5f501f8fd6c90aba36cc3150bd726a791fee315a27bae#0
    --password-of-payer
    --uri=ws://panarea.hotmoka.io:8001
Enter value for --password-of-payer (the password of the key pair of the payer account): orange
Do you really want to call method public void ...GradualPonzi.invest(java.math.BigInteger)
  spending up to 200000 gas units at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y
Adding transaction f46e68a534589213d5df03af52498880aeaee3d25b8243ec12395993f57844d3_invest... done.

Gas consumption:
 * total: 7546
   * for CPU: 2705
   * for RAM: 4323
   * for storage: 518
   * for penalty: 0
 * price per unit: 1 pana
 * total price: 7546 panas

$ moka objects call f544a0fa918fa2102bf59a9106e03c9c51f1d7d348930bd9e0b680aaef5c98ed#0
    io.hotmoka.tutorial.examples.ponzi.GradualPonzi
    invest
    15000
    --classpath=7a81dd5e19708146ba81fe639a048f3b0f56aed4362fc473fada31e487bd142a
    --receiver=38dfb6f1dbe91cc4c6f5f501f8fd6c90aba36cc3150bd726a791fee315a27bae#0
    --password-of-payer
    --uri=ws://panarea.hotmoka.io:8001
Enter value for --password-of-payer (the password of the key pair of the payer account): apple
Do you really want to call method public void ...GradualPonzi.invest(java.math.BigInteger)
  spending up to 200000 gas units at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y
Adding transaction f544a0fa918fa2102bf59a9106e03c9c51f1d7d348930bd9e0b680aaef5c98ed_invest... done.

Gas consumption: ...
```

We let the first player try to invest again in the contract, this time
with a too small investment, which leads to an exception,
since the code of the contract requires a minimum investment:

```shell
$ moka objects call cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    io.hotmoka.tutorial.examples.ponzi.GradualPonzi
    invest
    500
    --classpath=7a81dd5e19708146ba81fe639a048f3b0f56aed4362fc473fada31e487bd142a
    --receiver=38dfb6f1dbe91cc4c6f5f501f8fd6c90aba36cc3150bd726a791fee315a27bae#0
    --password-of-payer
    --uri=ws://panarea.hotmoka.io:8001

Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate
Do you really want to call method public void ...GradualPonzi.invest(java.math.BigInteger)
  spending up to 200000 gas units at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y
Adding transaction cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216_invest... failed.
The transaction failed with message io.takamaka.code.lang.RequirementViolationException: you must invest at least 1000@GradualPonzi.java:49

Gas consumption: ...
```

This exception states that a transaction failed because the last
investor invested less than 1000 units of coin. Note that the
exception message reports the cause (a `require` failed)
and includes the source program line
of the contract where the exception occurred:
line 49 of `GradualPonzi.java`, that is

```java
require(BigIntegerSupport.compareTo(amount, MINIMUM_INVESTMENT) >= 0,
  () -> StringSupport.concat("you must invest at least ", MINIMUM_INVESTMENT));
```

Finally, we can check the state of the contract:

```shell
$ moka objects show 38dfb6f1dbe91cc4c6f5f501f8fd6c90aba36cc3150bd726a791fee315a27bae#0
    --uri ws://panarea.hotmoka.io:8001
class io.hotmoka.tutorial.examples.ponzi.GradualPonzi
    (from jar installed at 7a81dd5e19708146ba81fe639a048f3b0f56aed4362fc473fada31e487bd142a)
  MINIMUM_INVESTMENT:java.math.BigInteger = 1000
  investors:io.takamaka.code.util.StorageList
    = 38dfb6f1dbe91cc4c6f5f501f8fd6c90aba36cc3150bd726a791fee315a27bae#1
  io.takamaka.code.lang.Contract.balance:java.math.BigInteger = 0
```
You can see that the contract keeps no balance. Moreover, its `investors` field is bound to an
object, whose state can be further investigated:

```shell
$ moka objects show 38dfb6f1dbe91cc4c6f5f501f8fd6c90aba36cc3150bd726a791fee315a27bae#1
    --uri ws://panarea.hotmoka.io:8001

class io.takamaka.code.util.StorageLinkedList (from jar installed at
    48a393014e839351b1bf56bdf9f127601f49b938d27e2c890fc8dd3e08099182)
  first:io.takamaka.code.util.StorageLinkedList$Node
    = 38dfb6f1dbe91cc4c6f5f501f8fd6c90aba36cc3150bd726a791fee315a27bae#2
  last:io.takamaka.code.util.StorageLinkedList$Node
    = e3b773332b409354bf4d1ab094b2e35380aaef1585e9e05a23fef88e238a96ef#0
  size:int = 3
```
As you can see, it is a `StorageLinkedList` of size three, since it contains our three accounts that interacted with the
`GradualPonzi` contract instance.

## Storage Arrays

Arrays are an ordered sequence of elements, with constant-time access
to such elements, both for reading and for writing. The size of the arrays is typically
fixed, although there are programming languages with limited forms
of dynamic arrays.

Java has native arrays, of type `E[]`, where `E` is the
type of the elements of the array. They can be used in Takamaka, but not
as fields of storage classes. For that, Takamaka provides class
`io.takamaka.code.util.StorageTreeArray<E>`. Its instances are storage objects and
can consequently be held in fields of storage classes and
can be stored in the store of a Hotmoka node, *as long as only
storage objects are added to the array*. Their size is fixed and decided
at the time of construction. Although we consider `StorageTreeArray<E>` as the storage
replacement for Java arrays, it must be stated that the complexity of
accessing their elements is logarithmic in the size of the array, which is
a significant deviation from the standard definition of arrays. Nevertheless,
logarithmic complexity is much better than the linear complexity for
accessing elements of a `StorageLinkedList<E>` that, instead, has the advantage
of being dynamic in size.

 <p align="center"><img width="600" src="pics/arrays.png" alt="Figure 25. The hierarchy of storage arrays"></p><p align="center">Figure 25. The hierarchy of storage arrays.</p>


We refer to the JavaDoc of `StorageTreeArray<E>` for a full list of its methods.
They include methods for adding elements, for accessing and
removing elements, for iterating on an array and for building a Java array
`E[]` with the elements of a `StorageTreeArray<E>`.
Figure 25 shows the hierarchy of the `StorageTreeArray<E>` class.
It implements the interface `StorageArray<E>`, that defines the methods that modify an array.
That interface extends the interface `StorageArrayView<E>` that, instead, defines the methods
that read data from an array, but do not modify it. This distinction between the _read-only_
interface and the _modification_ interface is identical to what we have seen for lists in the previous
sections. Arrays have methods `snapshot()` and `view()` as well, like lists. They yield `@Exported`
storage arrays, both in constant time. All constructors of the `StorageTreeArray<E>` class require to specify the immutable
size of the array. Moreover, it is possible to specify a default value for the elements of the
array, that can be explicit or given as a supplier, possibly indexed.

Next section shows an example of use for `StorageTreeArray<E>`.

### A Tic-Tac-Toe Contract

__[See `io-hotmoka-tutorial-examples-tictactoe` in `https://github.com/Hotmoka/hotmoka`]__

Tic-tac-toe is a game where two players place, alternately,
a cross and a circle on a 3x3 board, initially empty. The winner is the
player who places three crosses or three circles on the same row,
column or diagonal. For instance, in Figure 26 the player of
the cross wins.

 <p align="center"><img width="200" height="200" src="pics/tictactoe_wins.png" alt="Figure 26. Cross wins"></p><p align="center">Figure 26. Cross wins.</p>


There are games that end up in a draw, when the board is full but nobody wins,
as in Figure 27.

 <p align="center"><img width="250" height="250" src="pics/tictactoe_draw.png" alt="Figure 27. A draw"></p><p align="center">Figure 27. A draw.</p>


A natural representation of the tic-tac-toe board is a two-dimensional array
where indexes are distributed as shown in Figure 28.

 <p align="center"><img width="250" height="250" src="pics/tictactoe_grid.png" alt="Figure 28. A two-dimensional representation of the game"></p><p align="center">Figure 28. A two-dimensional representation of the game.</p>


This can be implemented as a `StorageTreeArray<StorageTreeArray<Tile>>`, where `Tile` is
a class that enumerates the three possible tiles (empty, cross, circle). This is
possible but overkill. It is simpler and cheaper (also in terms of gas)
to use the previous diagram as a conceptual representation of the board
shown to the users, but use, internally,
a one-dimensional array of nine tiles, distributed as in Figure 29.
This one-dimensional array can be implemented as a `StorageTreeArray<Tile>`. There will be functions
for translating the conceptual representation into the internal one.

 <p align="center"><img width="220" src="pics/tictactoe_grid_linear.png" alt="Figure 29. A linear representation of the game"></p><p align="center">Figure 29. A linear representation of the game.</p>


Create hence in Eclipse a new Maven Java 21 (or later) project named `io-hotmoka-tutorial-examples-tictactoe`.
You can do this by duplicating the project `io-hotmoka-tutorial-examples-family`.
Use the following `pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
    http://maven.apache.org/xsd/maven-4.0.0.xsd">

  <modelVersion>4.0.0</modelVersion>
  <groupId>io.hotmoka</groupId>
  <artifactId>io-hotmoka-tutorial-examples-tictactoe</artifactId>
  <version>1.9.0</version>

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.release>21</maven.compiler.release>
  </properties>

  <dependencies>
    <dependency>
      <groupId>io.hotmoka</groupId>
      <artifactId>io-takamaka-code</artifactId>
      <version>1.5.0</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.11.0</version>
      </plugin>
    </plugins>
  </build>

</project>
```

and the following `module-info.java`:

```java
module tictactoe {
  requires io.takamaka.code;
}
```

Create package `io.hotmoka.tutorial.examples.tictactoe` inside `src/main/java` and add
the following `TicTacToe.java` source inside that package:

```java
package io.hotmoka.tutorial.examples.tictactoe;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.StringSupport;
import io.takamaka.code.lang.View;
import io.takamaka.code.math.BigIntegerSupport;
import io.takamaka.code.util.StorageTreeArray;

public class TicTacToe extends Contract {

  @Exported
  public class Tile extends Storage {
    private final char c;

    private Tile(char c) {
      this.c = c;
    }

    @Override
    public String toString() {
      return String.valueOf(c);
    }

    private Tile nextTurn() {
      return this == CROSS ? CIRCLE : CROSS;
    }
  }

  private final Tile EMPTY = new Tile(' ');
  private final Tile CROSS = new Tile('X');
  private final Tile CIRCLE = new Tile('O');

  private final StorageTreeArray<Tile> board = new StorageTreeArray<>(9, EMPTY);
  private PayableContract crossPlayer;
  private PayableContract circlePlayer;
  private Tile turn = CROSS; // cross plays first
  private boolean gameOver;

  public @View Tile at(int x, int y) {
    require(1 <= x && x <= 3 && 1 <= y && y <= 3, "coordinates must be between 1 and 3");
    return board.get((y - 1) * 3 + x - 1);
  }

  private void set(int x, int y, Tile tile) {
    board.set((y - 1) * 3 + x - 1, tile);
  }

  public @Payable @FromContract(PayableContract.class) void play(long amount, int x, int y) {
    require(!gameOver, "the game is over");
    require(1 <= x && x <= 3 && 1 <= y && y <= 3, "coordinates must be between 1 and 3");
    require(at(x, y) == EMPTY, "the selected tile is not empty");

    PayableContract player = (PayableContract) caller();

    if (turn == CROSS)
      if (crossPlayer == null)
        crossPlayer = player;
      else
        require(player == crossPlayer, "it's not your turn");
    else
      if (circlePlayer == null) {
        require(crossPlayer != player, "you cannot play against yourself");
        long previousBet = BigIntegerSupport.subtract
          (balance(), BigInteger.valueOf(amount)).longValue();
        require(amount >= previousBet,
          () -> StringSupport.concat("you must bet at least ", previousBet, " coins"));
        circlePlayer = player;
      }
      else
        require(player == circlePlayer, "it's not your turn");

    set(x, y, turn);
    if (isGameOver(x, y))
      player.receive(balance());
    else
      turn = turn.nextTurn();
  }

  private boolean isGameOver(int x, int y) {
    if (at(x, 1) == turn && at(x, 2) == turn && at(x, 3) == turn) // column x
      return gameOver = true;

    if (at(1, y) == turn && at(2, y) == turn && at(3, y) == turn) // row y
      return gameOver = true;

    if (x == y && at(1, 1) == turn && at (2, 2) == turn && at(3, 3) == turn) // first diagonal
      return gameOver = true;

    if (x + y == 4 && at(1, 3) == turn && at(2, 2) == turn && at(3, 1) == turn) // second diagonal
      return gameOver = true;

    return gameOver = false;
  }

  @Override
  public @View String toString() {
    return StringSupport.concat(at(1, 1), "|", at(2, 1), "|", at(3, 1),
      "\n-----\n", at(1, 2), "|", at(2, 2), "|", at(3, 2),
      "\n-----\n", at(1, 3), "|", at(2, 3), "|", at(3, 3));
  }
}
```

The internal class `Tile` represents the three alternatives that can be
put in the tic-tac-toe board. It overrides the default
`toString()` implementation, to yield the
usual representation for such alternatives; its `nextTurn()` method
alternates between cross and circle.

The board of the game is represented as a `new StorageTreeArray<>(9, EMPTY)`, whose
elements are indexed from 0 to 8 (inclusive) and are initialized to `EMPTY`.
It is also possible to construct the array as `new StorageTreeArray<>(9)`, but then
its elements would hold the default value `null` and the array would need to be initialized
inside a constructor for `TicTacToe`.

Methods `at()` and `set()` read and set the board element
at indexes (x,y), respectively. They transform the two-dimensional conceptual representation
of the board into its internal one-dimensional representation. Since `at()` is `public`,
we defensively check the validity of the indexes there.

Method `play()` is the heart of the contract. It is called by the accounts
that play the game, hence it is annotated as `@FromContract`. It is also annotated as
`@Payable(PayableContract.class)` since players must bet money for
taking part in the game, at least for the first two moves, and receive
money if they win. The first
contract that plays is registered as `crossPlayer`. The second contract
that plays is registered as `circlePlayer`. Subsequent moves must
come, alternately, from `crossPlayer` and `circlePlayer`. The contract
uses a `turn` variable to keep track of the current turn.

Note the extensive use of `require()` to check all error situations:

1. It is possible to play only if the game is not over yet.
2. A move must be inside the board and identify an empty tile.
3. Players must alternate correctly.
4. The second player must bet at least as much as the first player.
5. It is not allowed to play against oneself.

The `play()` method ends with a call to `gameOver()` that checks
if the game is over, that is, if the current player won.
In that case, the winner receives the full
jackpot. Note that the `gameOver()` method receives the coordinates
where the current player has moved. This allows it to restrict the
check for game over: the game is over only if the row or column
where the player moved contain the same tile; if the current player
played on a diagonal, the method checks the diagonals as well.
It is of course possible to check all rows, columns and diagonals, always,
but our solution is gas-thriftier.

The `toString()` method yields a string representation of the current board, such as

```
X|O| 
-----
 |X|O
-----
 |X| 
```

### A More Realistic Tic-Tac-Toe Contract

__[See `io-hotmoka-tutorial-examples-tictactoe_revised` in `https://github.com/Hotmoka/hotmoka`]__

The `TicTacToe.java` code implements the rules of a tic-tac-toe game, but has
a couple of drawbacks that make it still incomplete. Namely:

1. The creator of the game must spend gas to call its constructor,
   but has no direct incentive in doing so. He must be a benefactor,
   or hope to take part in the game after creation, if he is faster than
   any other potential player.
2. If the game ends in a draw, money gets stuck in the `TicTacToe` contract
   instance, for ever and ever.

Replace hence the previous version of `TicTacToe.java` with the following
revised version. This new version solves
both problems at once. The policy is very simple: it imposes a minimum
bet, in order to avoid free games; if a winner emerges,
then the game forwards him only 90% of the jackpot; the remaining 10% goes to the
creator of the `TicTacToe` contract. If, instead, the game ends in a draw,
it forwards the whole jackpot to the creator.
Note that we added a `@FromContract` constructor, that takes
note of the `creator` of the game:

```java
package io.hotmoka.tutorial.examples.tictactoe;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.StringSupport;
import io.takamaka.code.lang.View;
import io.takamaka.code.math.BigIntegerSupport;
import io.takamaka.code.util.StorageTreeArray;

public class TicTacToe extends Contract {

  @Exported
  public class Tile extends Storage {
    private final char c;

    private Tile(char c) {
      this.c = c;
    }

    @Override
    public String toString() {
      return String.valueOf(c);
    }

    private Tile nextTurn() {
      return this == CROSS ? CIRCLE : CROSS;
    }
  }

  private final Tile EMPTY = new Tile(' ');
  private final Tile CROSS = new Tile('X');
  private final Tile CIRCLE = new Tile('O');

  private static final long MINIMUM_BET = 100L;

  private final StorageTreeArray<Tile> board = new StorageTreeArray<>(9, EMPTY);
  private final PayableContract creator;
  private PayableContract crossPlayer;
  private PayableContract circlePlayer;
  private Tile turn = CROSS; // cross plays first
  private boolean gameOver;

  public @FromContract(PayableContract.class) TicTacToe() {
    creator = (PayableContract) caller();
  }

  public @View Tile at(int x, int y) {
    require(1 <= x && x <= 3 && 1 <= y && y <= 3, "coordinates must be between 1 and 3");
    return board.get((y - 1) * 3 + x - 1);
  }

  private void set(int x, int y, Tile tile) {
    board.set((y - 1) * 3 + x - 1, tile);
  }

  public @Payable @FromContract(PayableContract.class) void play(long amount, int x, int y) {
    require(!gameOver, "the game is over");
    require(1 <= x && x <= 3 && 1 <= y && y <= 3, "coordinates must be between 1 and 3");
    require(at(x, y) == EMPTY, "the selected tile is not empty");

    PayableContract player = (PayableContract) caller();

    if (turn == CROSS)
      if (crossPlayer == null) {
        require(amount >= MINIMUM_BET, () -> "you must invest at least " + MINIMUM_BET + " coins");
         crossPlayer = player;
      }
      else
        require(player == crossPlayer, "it's not your turn");
    else
      if (circlePlayer == null) {
        require(crossPlayer != player, "you cannot play against yourself");
        long previousBet = BigIntegerSupport.subtract
          (balance(), BigInteger.valueOf(amount)).longValue();
        require(amount >= previousBet,
          () -> StringSupport.concat("you must bet at least ", previousBet, " coins"));
        circlePlayer = player;
      }
      else
        require(player == circlePlayer, "it's not your turn");

    set(x, y, turn);
    if (isGameOver(x, y)) {
      // 90% goes to the winner
      player.receive(BigIntegerSupport.divide
        (BigIntegerSupport.multiply(balance(), BigInteger.valueOf(9L)), BigInteger.valueOf(10L)));
      // the rest to the creator of the game
      creator.receive(balance());
    }
    else if (isDraw())
      // everything goes to the creator of the game
      creator.receive(balance());
    else
      turn = turn.nextTurn();
  }

  private boolean isGameOver(int x, int y) {
    if (at(x, 1) == turn && at(x, 2) == turn && at(x, 3) == turn) // column x
      return gameOver = true;

    if (at(1, y) == turn && at(2, y) == turn && at(3, y) == turn) // row y
      return gameOver = true;

    if (x == y && at(1, 1) == turn && at (2, 2) == turn && at(3, 3) == turn) // first diagonal
      return gameOver = true;

    if (x + y == 4 && at(1, 3) == turn && at(2, 2) == turn && at(3, 1) == turn) // second diagonal
      return gameOver = true;

    return gameOver = false;
  }

  private boolean isDraw() {
    for (var tile: board)
      if (tile == EMPTY)
        return false;

    return true;
  }

  @Override
  public @View String toString() {
    return StringSupport.concat(at(1, 1), "|", at(2, 1), "|", at(3, 1),
      "\n-----\n", at(1, 2), "|", at(2, 2), "|", at(3, 2),
      "\n-----\n", at(1, 3), "|", at(2, 3), "|", at(3, 3));
  }
}
```

> We have chosen to allow a `long amount` in the `@Payable` method `play()` since
> it is unlikely that users will want to invest huge quantities of money in this
> game. This gives us the opportunity to discuss why the computation of the
> previous bet has been written as
> `long previousBet = BigIntegerSupport.subtract(balance(), BigInteger.valueOf(amount)).longValue()`
> instead of the simpler
> `long previousBet = balance().longValue() - amount`.
> The reason is that, when that line is executed, both players have already paid
> their bet, that accumulates in the balance of the `TicTacToe` contract.
> Each single bet is a `long`, but their sum could overflow the size of a `long`.
> Hence, we have to deal with a computation on `BigInteger`. The same situation
> occurs later, when we have to compute the 90% that goes to the winner:
> the jackpot might be larger than a `long` and we have to compute over
> `BigInteger`. As a final remark, note that in the line:
> `BigIntegerSupport.divide(BigIntegerSupport.multiply(balance(), BigInteger.valueOf(9L)), BigInteger.valueOf(10L))`
> we first multiply by 9 and **then** divide by 10. This reduces the
> approximation inherent to integer division. For instance, if the jackpot
> (`balance()`) were 209, we have (with Java's left-to-right evaluation)
> `
> 209*9/10=1881/10=188
> `
> while
> `
> 209/10*9=20*9=180
> `.

### Running the Tic-Tac-Toe Contract

Let us play with the `TicTacToe` contract. Go inside the `io-hotmoka-tutorial-examples-tictactoe` project,
compile it with Maven and store it in the Hotmoka node:

```shell
$ cd hotmoka_tutorial/io-hotmoka-tutorial-examples-tictactoe   # if not already there
$ mvn install
$ cd ..
$ moka jars install cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    io-hotmoka-tutorial-examples-tictactoe/target/io-hotmoka-tutorial-examples-tictactoe-1.9.0.jar
    --password-of-payer
    --uri ws://panarea.hotmoka.io:8001

Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate
Do you really want to install the jar spending up to 1268400 gas units
  at the price of 1 pana per unit (that is, up to 1268400 panas) [Y/N] Y
Adding transaction 624c2d3ae5be809a054beaacc83e16ddc1efc8367646de0295e04e420db2e649... done.
The jar has been installed at 624c2d3ae5be809a054beaacc83e16ddc1efc8367646de0295e04e420db2e649.

Gas consumption:
 * total: 15815
   * for CPU: 1652
   * for RAM: 3446
   * for storage: 10717
   * for penalty: 0
 * price per unit: 1 pana
 * total price: 15815 panas
```

Then we create an instance of the contract in the node:

```shell
$ moka objects create cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    io.hotmoka-tutorial.examples.tictactoe.TicTacToe
    --classpath=624c2d3ae5be809a054beaacc83e16ddc1efc8367646de0295e04e420db2e649
    --password-of-payer
    --uri ws://panarea.hotmoka.io:8001

Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate
Do you really want to call constructor public io.hotmoka.tutorial.examples.tictactoe.TicTacToe()
  spending up to 200000 gas units at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y
Adding transaction 8bfddbd3ddfb766c701e0fbc055ed05e2f21008fcacefda40478924034e67697... done.
A new object 8bfddbd3ddfb766c701e0fbc055ed05e2f21008fcacefda40478924034e67697#0 has been created.

Gas consumption:
 * total: 24320
   * for CPU: 9249
   * for RAM: 14173
   * for storage: 898
   * for penalty: 0
 * price per unit: 1 pana
 * total price: 24320 panas
```

We use two of our accounts now, that we have already created in the previous section,
to interact with the contract: they will play, alternately, until the first player wins.
We will print the `toString` of the contract after each move.

The first player starts, by playing at (1,1), and bets 100:

```shell
$ moka objects call cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    io.hotmoka.tutorial.examples.tictactoe.TicTacToe
    play
    100 1 1
    --password-of-payer
    --uri ws://panarea.hotmoka.io:8001
    --receiver=8bfddbd3ddfb766c701e0fbc055ed05e2f21008fcacefda40478924034e67697#0

Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate 
Do you really want to call method public void ...TicTacToe.play(long,int,int)
  spending up to 200000 gas units at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y
Adding transaction f363520b39cf350f5a7c99dacf9f4608befd68d94a97c625e8e31237776c3cdf... done.

Gas consumption:
 * total: 9918
   * for CPU: 3890
   * for RAM: 5405
   * for storage: 623
   * for penalty: 0
 * price per unit: 1 pana
 * total price: 9918 panas

$ moka objects call cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    io.hotmoka.tutorial.examples.tictactoe.TicTacToe toString
    --uri ws://panarea.hotmoka.io:8001
    --receiver=8bfddbd3ddfb766c701e0fbc055ed05e2f21008fcacefda40478924034e67697#0

Running transaction 130cac8c0144a00f421bc52dc1a97f5f28415415bb2fc1e0ab2d67935c7db12d... done.
The method returned:
X| | 
-----
 | | 
-----
 | |
```

Note that the call to `toString()` does not require to provide the password of the key pair of the caller account,
since that method is a `@View` method, hence `moka` runs a transaction to call it, rather than adding a transaction.

The second player plays now, at (2,1), betting 100:

```shell
$ moka objects call f46e68a534589213d5df03af52498880aeaee3d25b8243ec12395993f57844d3#0
    io.hotmoka.tutorial.examples.tictactoe.TicTacToe 
    play
    100 2 1
    --password-of-payer
    --uri ws://panarea.hotmoka.io:8001
    --receiver=8bfddbd3ddfb766c701e0fbc055ed05e2f21008fcacefda40478924034e67697#0

Enter value for --password-of-payer (the password of the key pair of the payer account): orange
Do you really want to call method public void ...TicTacToe.play(long,int,int)
  spending up to 200000 gas units at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y
Adding transaction c5c2c8bc5b49b35ad0a38b34ff49250d8d8ade0acdd2e05b060fe42da7067bc2... done.
...

$ moka objects call f46e68a534589213d5df03af52498880aeaee3d25b8243ec12395993f57844d3#0
    io.hotmoka.tutorial.examples.tictactoe.TicTacToe toString
    --uri ws://panarea.hotmoka.io:8001
    --receiver=8bfddbd3ddfb766c701e0fbc055ed05e2f21008fcacefda40478924034e67697#0

Running transaction d6268201f84e47d0c40236f9aaad9a24a1f4a41ff7cf45fdb8a6ce96f9ed68ae... done.
The method returned:
X|O| 
-----
 | | 
-----
 | |
```

The first player replies, playing at (1,2):

```shell
$ moka objects call cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    io.hotmoka.tutorial.examples.tictactoe.TicTacToe
    play
    0 1 2
    --password-of-payer
    --uri ws://panarea.hotmoka.io:8001
    --receiver=8bfddbd3ddfb766c701e0fbc055ed05e2f21008fcacefda40478924034e67697#0

Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate
Do you really want to call method public void ...TicTacToe.play(long,int,int)
  spending up to 200000 gas units at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y
Adding transaction dbc67d8be8fb1bb1d6a9f89b8a6ef5aefe59bf8dd4fabf7da8de7ca3e3ef9610... done.
...

$ moka objects call cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    io.hotmoka.tutorial.examples.tictactoe.TicTacToe toString
    --uri ws://panarea.hotmoka.io:8001
    --receiver=8bfddbd3ddfb766c701e0fbc055ed05e2f21008fcacefda40478924034e67697#0

Running transaction 130cac8c0144a00f421bc52dc1a97f5f28415415bb2fc1e0ab2d67935c7db12d... done.
The method returned:
X|O| 
-----
X| | 
-----
 | |
```

Then the second player plays at (2,2):

```shell
$ moka objects call f46e68a534589213d5df03af52498880aeaee3d25b8243ec12395993f57844d3#0
    io.hotmoka.tutorial.examples.tictactoe.TicTacToe
    play
    100 2 2
    --password-of-payer
    --uri ws://panarea.hotmoka.io:8001
    --receiver=8bfddbd3ddfb766c701e0fbc055ed05e2f21008fcacefda40478924034e67697#0

Enter value for --password-of-payer (the password of the key pair of the payer account): orange
Do you really want to call method public void ...TicTacToe.play(long,int,int)
  spending up to 200000 gas units at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y
Adding transaction f7f04d96eb05a10bfe89b414bc14918d23d868664d30adc1331f7c14b4cdaf17... done.
...

$ moka objects call f46e68a534589213d5df03af52498880aeaee3d25b8243ec12395993f57844d3#0
    io.hotmoka.tutorial.examples.tictactoe.TicTacToe toString
    --uri ws://panarea.hotmoka.io:8001
    --receiver=8bfddbd3ddfb766c701e0fbc055ed05e2f21008fcacefda40478924034e67697#0

Running transaction d6268201f84e47d0c40236f9aaad9a24a1f4a41ff7cf45fdb8a6ce96f9ed68ae... done.
The method returned:
X|O| 
-----
X|O| 
-----
 | |
```

The first player wins by playing at (1,3):

```shell
$ moka objects call cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    io.hotmoka.tutorial.examples.tictactoe.TicTacToe
    play
    0 1 3
    --password-of-payer
    --uri ws://panarea.hotmoka.io:8001
    --receiver=8bfddbd3ddfb766c701e0fbc055ed05e2f21008fcacefda40478924034e67697#0

Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate
Do you really want to call method public void ...TicTacToe.play(long,int,int)
  spending up to 200000 gas units at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y
Adding transaction 6cdb536b88e5b04f16180de033c3953386c58a31aad44c805ba3b65541961576... done.
...

$ moka objects call cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    io.hotmoka.tutorial.examples.tictactoe.TicTacToe toString
    --uri ws://panarea.hotmoka.io:8001
    --receiver=8bfddbd3ddfb766c701e0fbc055ed05e2f21008fcacefda40478924034e67697#0

Running transaction 130cac8c0144a00f421bc52dc1a97f5f28415415bb2fc1e0ab2d67935c7db12d... done.
The method returned:
X|O| 
-----
X|O| 
-----
X| |
```
We can verify that the game is over now:

```shell
$ moka objects show 8bfddbd3ddfb766c701e0fbc055ed05e2f21008fcacefda40478924034e67697#0
    --uri ws://panarea.hotmoka.io:8001

class io.hotmoka.tutorial.examples.tictactoe.TicTacToe
    (from jar installed at 624c2d3ae5be809a054beaacc83e16ddc1efc8367646de0295e04e420db2e649)
  CIRCLE:...TicTacToe$Tile = ...
  CROSS:...TicTacToe$Tile = ...
  EMPTY:...TicTacToe$Tile = ...
  board:io.takamaka.code.util.StorageTreeArray = ...
  circlePlayer:io.takamaka.code.lang.PayableContract
    = f46e68a534589213d5df03af52498880aeaee3d25b8243ec12395993f57844d3#0
  crossPlayer:io.takamaka.code.lang.PayableContract
    = cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
  creator:io.takamaka.code.lang.PayableContract
    = cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
  gameOver:boolean = true
  turn:...TicTacToe$Tile = ...
  io.takamaka.code.lang.Contract.balance:java.math.BigInteger = 0
```
As you can see, the balance of the contract is zero since it has been distributed to
the winner and to the creator of the game (that actually coincide to our first account,
in this specific run).

If the second player attempts to play now, the transaction will be rejected, since the game is over:

```shell
$ moka objects call f46e68a534589213d5df03af52498880aeaee3d25b8243ec12395993f57844d3#0
    io.hotmoka.tutorial.examples.tictactoe.TicTacToe
    play
    0 2 3
    --password-of-payer
    --uri ws://panarea.hotmoka.io:8001
    --receiver=8bfddbd3ddfb766c701e0fbc055ed05e2f21008fcacefda40478924034e67697#0

Enter value for --password-of-payer (the password of the key pair of the payer account): orange
Do you really want to call method public void ...TicTacToe.play(long,int,int)
  spending up to 200000 gas units at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y
Adding transaction bc3dc45be4c350b1ed4558341d47729fb1f95f5437d357978eac623bba319a87... failed.
The transaction failed with message io.takamaka.code.lang.RequirementViolationException:
  the game is over@TicTacToe.java:84

Gas consumption:
 * total: 200000
   * for CPU: 1991
   * for RAM: 3597
   * for storage: 230
   * for penalty: 194182
 * price per unit: 1 pana
 * total price: 200000 panas
```

### Specialized Storage Array Classes

The `StorageTreeArray<E>` class is very general, since it can be used to hold
any type `E` of storage values. Since it uses generics,
primitive values cannot be held in a `StorageTreeArray<E>`, directly.
For instance, `StorageTreeArray<byte>` is not legal syntax in Java.
Instead, one could think to use `StorageTreeArray<Byte>`, where `Byte`
is the Java wrapper class `java.lang.Byte`. However, that class is not
currently allowed in storage, hence `StorageTreeArray<Byte>` will not work either.
One should hence define a new wrapper class for `byte`, that extends `Storage`.
That is possible, but highly discouraged:
the use of wrapper classes introduces a level of indirection
and requires the instantiation of many small objects, which costs gas. Instead,
Takamaka provides specialized storage classes implementing arrays of bytes,
without wrappers. The rationale is that such arrays arise
naturally when dealing, for instance, with hashes or encrypted data
(see next section for an example) and consequently deserve
a specialized and optimized implementation.
Such specialized array classes
can have their length specified at construction time, or fixed to
a constant (for best optimization and minimal gas consumption).

 <p align="center"><img width="700" src="pics/bytes.png" alt="Figure 30. Specialized byte array classes"></p><p align="center">Figure 30. Specialized byte array classes.</p>


Figure 30 shows the hierarchy of the specialized classes for arrays of bytes,
available in Takamaka.
The interface `StorageByteArrayView` defines the methods that read data from an array
of bytes, while the interface `StorageByteArray` defines the modification methods.
Class `StorageTreeByteArray` allows one to create byte arrays of any length,
specified at construction time.
Classes `Bytes32` and `Bytes32Snapshot` have, instead, fixed length of 32 bytes;
their constructors include one that allows one to specify such 32 bytes,
which is useful for calling the constructor from outside the node,
since `byte` is a storage type.
While a `Bytes32` is modifiable, instances of class `Bytes32Snapshot`
are not modifiable after being created and are `@Exported`.
There are sibling classes for different, fixed sizes, such as
`Bytes64` and `Bytes8Snaphot`. For a full description of the methods
of these classes and interfaces, we refer to their JavaDoc.

## Storage Maps

Maps are dynamic associations of objects to objects. They are useful
for programming smart contracts, as their extensive use in Solidity proves.
However, most such uses are related to the withdrawal pattern, that is
not needed in Takamaka. Nevertheless, there are still situations when
maps are useful in Takamaka code, as we show below.

Java has many implementations of maps.
However, they are not storage objects and consequently cannot be
stored in a Hotmoka node. This section describes the
`io.takamaka.code.util.StorageTreeMap<K,V>` class, that extends `Storage` and
whose instances can then be held in the store of a node, if keys `K` and
values `V` can be stored in a node as well.

We refer to the JavaDoc of `StorageTreeMap` for a full description of its methods,
that are similar to those of traditional Java maps. Here, we just observe
that a key is mapped into a value by calling method
`void put(K key, V value)`, while the value bound to a key is retrieved by calling
`V get(Object key)`. It is possible to yield a default value when a key is not
in the map, by calling `V getOrDefault(Object key, V _default)` or
its sibling `V getOrDefault(Object key, Supplier<? extends V> _default)`, that
evaluates the default value only if needed. Method `V putIfAbsent(K key, V value)`,
binds the key to the value only if the key is unbound. Similarly for
its sibling `V computeIfAbsent(K key, Supplier<? extends V> value)` that, however,
evaluates the new value only if needed (these two methods differ for their
returned value, as in Java maps. Please refer to their JavaDoc).

Instances of `StorageTreeMap<K,V>` keep keys in increasing order. Namely, if
type `K` has a natural order, that order is used. Otherwise, keys
(that must be storage objects) are kept ordered by increasing storage
reference. Consequently, methods `forEach(Consumer<? super Entry<K,V>> action)`,
`forEachKey(Consumer<? super K> action)` and
`forEachValue(Consumer<? super V> action)`
perform an internal iteration of the elements of the map, in order.

> Compare this with Solidity, where maps do not know the set of their keys nor the
> set of their values.

 <p align="center"><img width="600" src="pics/maps.png" alt="Figure 31. The hierarchy of storage maps"></p><p align="center">Figure 31. The hierarchy of storage maps.</p>


Figure 31 shows the hierarchy of the `StorageTreeMap<K,V>` class.
It implements the interface `StorageMap<K,V>`, that defines the methods that modify a map.
That interface extends the interface `StorageMapView<K,V>` that, instead, defines the methods
that read data from a map, but do not modify it.
Methods `snapshot()` and `view()` return an `@Exported` `StorageMapView<K,V>`, in constant time.

There are also specialized map classes, optimized
for specific primitive types of keys, such as `StorageTreeIntMap<V>`,
whose keys are `int` values. We refer to their JavaDoc for further information.

### A Blind Auction Contract

__[See `io-hotmoka-tutorial-examples-auction` in `https://github.com/Hotmoka/hotmoka`]__

This section exemplifies the use of class `StorageTreeMap` for writing a smart
contract that implements a _blind auction_. That contract allows
a _beneficiary_ to sell an item to the buying contract that offers
the highest bid. Since data in blockchain is public, in a non-blind
auction it is possible that bidders eavesdrop the offers of other bidders
in order to place an offer that is only slightly higher than the current
best offer. A blind auction, instead, uses a two-phases
mechanism: in the initial _bidding time_, bidders place bids, hashed, so that
they do not reveal their amount. After the bidding time expires, the second
phase, called _reveal time_, allows bidders to
reveal the real values of their bids and the auction contract to determine
the actual winner.
This works since, to reveal a bid, each bidder provides the real data
of the bid. The auction contract then recomputes the hash from real data and
checks if the result matches the hash provided at bidding time.
If not, the bid is considered invalid. Bidders can even place fake offers
on purpose, in order to confuse other bidders.

Create in Eclipse a new Maven Java 21 (or later) project named `io-hotmoka-tutorial-examples-auction`.
You can do this by duplicating the project `io-hotmoka-tutorial-examples-family`.
Use the following `pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                        http://maven.apache.org/xsd/maven-4.0.0.xsd">

  <modelVersion>4.0.0</modelVersion>
  <groupId>io.hotmoka</groupId>
  <artifactId>io-hotmoka-tutorial-examples-auction</artifactId>
  <version>1.9.0</version>

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.release>21</maven.compiler.release>
  </properties>

  <dependencies>
    <dependency>
      <groupId>io.hotmoka</groupId>
      <artifactId>io-takamaka-code</artifactId>
      <version>1.5.0</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.11.0</version>
      </plugin>
    </plugins>
  </build>

</project>
```

and the following `module-info.java`:

```java
module auction {
  requires io.takamaka.code;
}
```

Create package `io.hotmoka.tutorial.examples.auction` inside `src/main/java` and add
the following `BlindAuction.java` inside that package.
It is a Takamaka contract that implements
a blind auction. Since each bidder may place more bids and since such bids
must be kept in storage until reveal time, this code uses a map
from bidders to lists of bids. This smart contract has been inspired
by a similar Solidity contract [[BlindAuction]](#references).
Please note that this code will not compile yet, since it misses two classes
that we will define in the next section.

```java
package io.hotmoka.tutorial.examples.auction;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.now;
import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.StringSupport;
import io.takamaka.code.math.BigIntegerSupport;
import io.takamaka.code.security.SHA256Digest;
import io.takamaka.code.util.Bytes32Snapshot;
import io.takamaka.code.util.StorageLinkedList;
import io.takamaka.code.util.StorageList;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageTreeMap;

/**
 * A contract for a simple auction. This class is derived from the Solidity code shown at
 * https://solidity.readthedocs.io/en/v0.5.9/solidity-by-example.html#id2
 * In this contract, bidders place bids together with a hash. At the end of
 * the bidding period, bidders are expected to reveal if and which of their bids
 * were real and their actual value. Fake bids are refunded. Real bids are compared
 * and the bidder with the highest bid wins.
 */
public class BlindAuction extends Contract {

  /**
   * A bid placed by a bidder. The deposit has been payed in full.
   * If, later, the bid will be revealed as fake, then the deposit will
   * be fully refunded. If, instead, the bid will be revealed as real, but for
   * a lower amount, then only the difference will be refunded.
   */
  private static class Bid extends Storage {

    /**
     * The hash that will be regenerated and compared at reveal time.
     */
    private final Bytes32Snapshot hash;

    /**
     * The value of the bid. Its real value might be lower and known
     * at real time only.
     */
    private final BigInteger deposit;

    private Bid(Bytes32Snapshot hash, BigInteger deposit) {
      this.hash = hash;
      this.deposit = deposit;
    }

    /**
     * Recomputes the hash of a bid at reveal time and compares it
     * against the hash provided at bidding time. If they match,
     * we can reasonably trust the bid.
     * 
     * @param revealed the revealed bid
     * @param digest the hasher
     * @return true if and only if the hashes match
     */
    private boolean matches(RevealedBid revealed, SHA256Digest digest) {
      digest.update(BigIntegerSupport.toByteArray(revealed.value));
      digest.update(revealed.fake ? (byte) 0 : (byte) 1);
      digest.update(revealed.salt.toArray());
      byte[] arr1 = hash.toArray();
      byte[] arr2 = digest.digest();

      if (arr1.length != arr2.length)
        return false;

      for (int pos = 0; pos < arr1.length; pos++)
        if (arr1[pos] != arr2[pos])
          return false;

      return true;
    }
  }

  /**
   * A bid revealed by a bidder at reveal time. The bidder shows
   * if the corresponding bid was fake or real, and how much was the
   * actual value of the bid. This might be lower than previously communicated.
   */
  @Exported
  public static class RevealedBid extends Storage {
    private final BigInteger value;
    private final boolean fake;

    /**
     * The salt used to strengthen the hashing.
     */
    private final Bytes32Snapshot salt;

    public RevealedBid(BigInteger value, boolean fake, Bytes32Snapshot salt) {
      this.value = value;
      this.fake = fake;
      this.salt = salt;
    }
  }

  /**
   * The beneficiary that, at the end of the reveal time, will receive the highest bid.
   */
  private final PayableContract beneficiary;

  /**
   * The bids for each bidder. A bidder might place more bids.
   */
  private final StorageMap<PayableContract, StorageList<Bid>> bids = new StorageTreeMap<>();

  /**
   * The time when the bidding time ends.
   */
  private final long biddingEnd;

  /**
   * The time when the reveal time ends.
   */
  private final long revealEnd;

  /**
   * The bidder with the highest bid, at reveal time.
   */
  private PayableContract highestBidder;

  /**
   * The highest bid, at reveal time.
   */
  private BigInteger highestBid;

  /**
   * Creates a blind auction contract.
   * 
   * @param biddingTime the length of the bidding time
   * @param revealTime the length of the reveal time
   */
  public @FromContract(PayableContract.class) BlindAuction(int biddingTime, int revealTime) {
    require(biddingTime > 0, "Bidding time must be positive");
    require(revealTime > 0, "Reveal time must be positive");

    this.beneficiary = (PayableContract) caller();
    this.biddingEnd = now() + biddingTime;
    this.revealEnd = biddingEnd + revealTime;
  }

  /**
   * Places a blinded bid the given hash.
   * The sent money is only refunded if the bid is correctly
   * revealed in the revealing phase. The bid is valid if the
   * money sent together with the bid is at least "value" and
   * "fake" is not true. Setting "fake" to true and sending
   * not the exact amount are ways to hide the real bid but
   * still make the required deposit. The same bidder can place multiple bids.
   */
  public @Payable @FromContract(PayableContract.class) void bid(BigInteger amount, Bytes32Snapshot hash) {
    onlyBefore(biddingEnd);
    bids.computeIfAbsent((PayableContract) caller(),
     (Supplier<? extends StorageList<Bid>>) StorageLinkedList::new).add(new Bid(hash, amount));
  }

  /**
   * Reveals a bid of the caller. The caller will get a refund for all correctly
   * blinded invalid bids and for all bids except for the totally highest.
   * 
   * @param revealed the revealed bid
   * @throws NoSuchAlgorithmException if the hashing algorithm is not available
   */
  public @FromContract(PayableContract.class) void reveal(RevealedBid revealed)
      throws NoSuchAlgorithmException {
    onlyAfter(biddingEnd);
    onlyBefore(revealEnd);
    PayableContract bidder = (PayableContract) caller();
    StorageList<Bid> bids = this.bids.get(bidder);
    require(bids != null && bids.size() > 0, "No bids to reveal");
    require(revealed != null, () -> "The revealed bid cannot be null");

    // any other hashing algorithm will do, as long as both bidder and auction contract use the same
    var digest = new SHA256Digest();
    // by removing the head of the list, it makes it impossible for the caller to re-claim the same deposits
    bidder.receive(refundFor(bidder, bids.removeFirst(), revealed, digest));
  }

  public PayableContract auctionEnd() {
    onlyAfter(revealEnd);
    PayableContract winner = highestBidder;
	
    if (winner != null) {
      beneficiary.receive(highestBid);
      event(new AuctionEnd(winner, highestBid));
      highestBidder = null;
    }

    return winner;
  }

  /**
   * Checks how much of the deposit should be refunded for a given bid.
   * 
   * @param bidder the bidder that placed the bid
   * @param bid the bid, as was placed at bidding time
   * @param revealed the bid, as was revealed later
   * @param digest the hashing algorithm
   * @return the amount to refund
   */
  private BigInteger refundFor(PayableContract bidder, Bid bid, RevealedBid revealed,
                               SHA256Digest digest) {
    if (!bid.matches(revealed, digest))
      // the bid was not actually revealed: no refund
      return BigInteger.ZERO;
    else if (!revealed.fake && BigIntegerSupport.compareTo(bid.deposit, revealed.value) >= 0
             && placeBid(bidder, revealed.value))
      // the bid was correctly revealed and is the best up to now: only the difference between promised and provided is refunded;
      // the rest might be refunded later if a better bid will be revealed
      return BigIntegerSupport.subtract(bid.deposit, revealed.value);
    else
      // the bid was correctly revealed and is not the best one: it is fully refunded
      return bid.deposit;
  }

  /**
   * Takes note that a bidder has correctly revealed a bid for the given value.
   * 
   * @param bidder the bidder
   * @param value the value, as revealed
   * @return true if and only if this is the best bid, up to now
   */
  private boolean placeBid(PayableContract bidder, BigInteger value) {
    if (highestBid != null && BigIntegerSupport.compareTo(value, highestBid) <= 0)
      // this is not the best bid seen so far
      return false;

    // if there was a best bidder already, its bid is refunded
    if (highestBidder != null)
      // Refund the previously highest bidder
      highestBidder.receive(highestBid);

    // take note that this is the best bid up to now
    highestBid = value;
    highestBidder = bidder;
    event(new BidIncrease(bidder, value));

    return true;
  }

  private static void onlyBefore(long when) {
    long diff = now() - when;
    require(diff <= 0, StringSupport.concat(diff, " ms too late"));
  }

  private static void onlyAfter(long when) {
    long diff = now() - when;
    require(diff >= 0, StringSupport.concat(-diff, " ms too early"));
  }
}
```

Let us discuss this (long) code, by starting from the inner classes.

Class `Bid` represents a bid placed by a contract that takes part in the auction.
This information will be stored in blockchain at bidding time, hence
it is known to all other participants. An instance of `Bid` contains
the `deposit` paid at time of placing the bid. This is not necessarily
the real value of the offer but must be at least as large as the real offer,
or otherwise the bid will be considered as invalid and rejected at reveal time. Instances
of `Bid` contain a `hash` consisting of 32 bytes. As already said, this will
be recomputed at reveal time and matched against the result.
Since arrays cannot be stored in blockchain, we use the storage class
`io.takamaka.code.util.Bytes32Snapshot` here, a library class that holds 32 bytes, as a
traditional array (see [Specialized Storage Array Classes](#specialized-storage-array-classes)).
It is well possible to use a `StorageArray` of a wrapper
of `byte` here, but `Bytes32Snapshot` is much more compact and its methods consume less gas.

Class `RevealedBid` describes a bid revealed after bidding time.
It contains the real value of the bid, the salt used to strengthen the
hashing algorithm and a boolean `fake` that, when true, means that the
bid must be considered as invalid, since it was only placed in order
to confuse other bidders. It is possible to recompute and check the hash of
a revealed bid through method `Bid.matches()`, that uses a given
hashing algorithm (`digest`, a Java `java.security.MessageDigest`) to
hash value, fake mark and salt into bytes, finally compared
against the hash provided at bidding time.

The `BlindAuction` contract stores the `beneficiary` of the auction.
It is the contract that created the auction and is consequently
initialized, in the constructor of `BlindAuction`, to its caller.
The constructor must be annotated as `@FromContract` because of that.
The same constructor receives the length of bidding time and reveal time, in
milliseconds. This allows the contract to compute the absolute ending time
for the bidding phase and for the reveal phase, stored into fields
`biddingEnd` and `revealEnd`, respectively.
Note, in the constructor of `BlindAuction`, the
use of the static method `io.takamaka.code.lang.Takamaka.now()`, that yields the
current time, as with the traditional `System.currentTimeMillis()` of Java
(that instead cannot be used in Takamaka code). Method `now()`, in a blockchain, yields the
time of creation of the block of the current transaction, as seen by its miner.
That time is reported in the block and hence is independent from the
machine that runs the contract, which guarantees determinism.

Method `bid()` allows a caller (the bidder) to place a bid during the bidding phase.
An instance of `Bid` is created and added to a list, specific to each
bidder. Here is where our map comes to help. Namely, field
`bids` holds a `StorageTreeMap<PayableContract, StorageList<Bid>>`,
that can be held in the store of a node since it is a storage map between storage keys
and storage values. Method `bid()` computes an empty list of bids if it is the
first time that a bidder places a bid. For that, it uses method
`computeIfAbsent()` of `StorageMap`. If it used method `get()`, it would
run into a null-pointer exception the first time a bidder places a bid.
That is, storage maps default to `null`, as all Java maps. (But differently to
Solidity maps, that provide a default value automatically when undefined.)

Method `reveal()` is called by each bidder during the reveal phase.
It accesses the `bids` placed by the bidder during the bidding time.
The method matches each revealed bid against the corresponding
list of bids for the player, by calling
method `refundFor()`, that determines how much of the deposit must be
refunded to the bidder. Namely, if a bid was fake or was not the best bid,
it must be refunded in full. If it was the best bid, it must be partially refunded
if the apparent `deposit` turns out to be higher than the actual value of the
revealed bid. While bids are refunded, method `placeBid` updates
the best bid information.

Method `auctionEnd()` is meant to be called after the reveal phase.
If there is a winner, it sends the highest bid to the beneficiary.

Note the use of methods `onlyBefore()` and `onlyAfter()` to guarantee
that some methods are only run at the right moment.

### Events

__[See `io-hotmoka-tutorial-examples-auction_events` in `https://github.com/Hotmoka/hotmoka`]__

The code in the previous section does not compile since it misses two
classes `BidIncrease.java` and `AuctionEnd.java`, that we report below.
Namely, the code of the blind auction contract contains some lines that generate
_events_, such as:

```java
event(new AuctionEnd(winner, highestBid));
```

Events are milestones that are saved in the store of a Hotmoka node.
From outside the node, it is possible to subscribe to specific events and get
notified as soon as an event of that kind occurs,
to trigger actions when that happens. In terms of the
Takamaka language, events are generated through the
`io.takamaka.code.lang.Takamaka.event(Event event)` method, that receives a parameter
of type `io.takamaka.code.lang.Event`. The latter is simply an abstract class that
extends `Storage`. Hence, events will
be stored in the node as part of the transaction that generated that event.
The constructor of class `Event` is annotated as `FromContract`, which allows one
to create events from the code of contracts only. The creating contract is available
through method `creator()` of class `Event`.

In our example, the `BlindAuction` class uses two events, that you can add
to the `auction` package and are defined as follows:

```java
package io.hotmoka.tutorial.examples.auction;

import java.math.BigInteger;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.View;

public class BidIncrease extends Event {
  public final PayableContract bidder;
  public final BigInteger amount;

  @FromContract BidIncrease(PayableContract bidder, BigInteger amount) {
    this.bidder = bidder;
    this.amount = amount;
  }

  public @View PayableContract getBidder() {
    return bidder;
  }

  public @View BigInteger getAmount() {
    return amount;
  }
}
```

and

```java
package io.hotmoka.tutorial.examples.auction;

import java.math.BigInteger;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.View;

public class AuctionEnd extends Event {
  public final PayableContract highestBidder;
  public final BigInteger highestBid;

  @FromContract AuctionEnd(PayableContract highestBidder, BigInteger highestBid) {
    this.highestBidder = highestBidder;
    this.highestBid = highestBid;
  }

  public @View PayableContract getHighestBidder() {
    return highestBidder;
  }

  public @View BigInteger getHighestBid() {
  return highestBid;
  }
}

```

Now that all classes have been completed, the project should compile.
Go inside the `io-hotmoka-tutorial-examples-auction` project and run `mvn install`.

### Running the Blind Auction Contract

__[See `io-hotmoka-tutorial-examples-runs` in `https://github.com/Hotmoka/hotmoka`]__

This section presents a Java class that connects to a Hotmoka node and runs the blind auction
contract of the previous section. We could run that contract on the `ws://panarea.hotmoka.io:8001` server,
but that Hotmoka node is based on a proof of space consensus, that generates a block every ten
seconds _on average_. This means that, for a transaction to be committed, one could have to wait
more, sometime up to one minute. This would make the test slow and would require larger windows
for the bidding and for the revealing phases. Instead, we use the `ws://panarea.hotmoka.io:8002` server,
that is a Hotmoka node based on a proof of stake consensus, that generates a block every four seconds.
This makes the test faster and the timings reliable. However, this means that we must first generate
some new accounts for our tests, since those that we generated before for
`ws://panarea.hotmoka.io:8001` do not exist in `ws://panarea.hotmoka.io:8002`. We do it as previously,
but swapping the server we are talking to:

```shell
$ moka keys create --name=account4.pem --password
Enter value for --password
  (the password that will be needed later to use the key pair): banana
...
$ moka accounts create faucet 50000000000000 account4.pem --password
    --uri ws://panarea.hotmoka.io:8002
Enter value for --password (the password of the key pair): banana
Adding transaction 9149d644f3c98ea8e6747690b7787f03e83c185f48b815570843a547eb5a3efd... done.
A new account 9149d644f3c98ea8e6747690b7787f03e83c185f48b815570843a547eb5a3efd#0 has been created.
...
$ moka keys create --name=account5.pem --password
Enter value for --password
  (the password that will be needed later to use the key pair): mango
...
$ moka accounts create faucet 50000000000000 account5.pem --password
    --uri ws://panarea.hotmoka.io:8002
Enter value for --password (the password of the key pair): mango
Adding transaction 2c9eddf45836994e81a8bc90bdfa04d38d823776ce25b40ef96feab7f9e14a42... done.
A new account 2c9eddf45836994e81a8bc90bdfa04d38d823776ce25b40ef96feab7f9e14a42#0 has been created.
...
$ moka keys create --name=account6.pem --password
Enter value for --password
  (the password that will be needed later to use the key pair): strawberry
...
$ moka accounts create faucet 50000000000000 account6.pem --password
    --uri ws://panarea.hotmoka.io:8002
Enter value for --password (the password of the key pair): strawberry
Adding transaction 073c69c124364d5ddc2cfc3c0dd581dcaa63c2d1bb435ad3c40f78de84b2455e... done.
A new account 073c69c124364d5ddc2cfc3c0dd581dcaa63c2d1bb435ad3c40f78de84b2455e#0 has been created.
...
```

Go to the `io-hotmoka-tutorial-examples-runs` Eclipse project and add the following
class inside its package:

```java
package io.hotmoka.tutorial.examples.runs;

import static io.hotmoka.helpers.Coin.panarea;
import static io.hotmoka.node.StorageTypes.BIG_INTEGER;
import static io.hotmoka.node.StorageTypes.BOOLEAN;
import static io.hotmoka.node.StorageTypes.BYTE;
import static io.hotmoka.node.StorageTypes.BYTES32_SNAPSHOT;
import static io.hotmoka.node.StorageTypes.INT;
import static io.hotmoka.node.StorageTypes.PAYABLE_CONTRACT;
import static io.hotmoka.node.StorageValues.byteOf;

import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import io.hotmoka.constants.Constants;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.helpers.api.GasHelper;
import io.hotmoka.helpers.api.NonceHelper;
import io.hotmoka.node.Accounts;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.remote.RemoteNodes;

public class Auction {

  public final static int NUM_BIDS = 10; // number of bids placed
  public final static int BIDDING_TIME = 230_000; // in milliseconds
  public final static int REVEAL_TIME = 350_000; // in milliseconds

  private final static BigInteger _500_000 = BigInteger.valueOf(500_000);

  private final static ClassType BLIND_AUCTION
    = StorageTypes.classNamed("io.hotmoka.tutorial.examples.auction.BlindAuction");
  private final static ConstructorSignature CONSTRUCTOR_BYTES32_SNAPSHOT
    = ConstructorSignatures.of(BYTES32_SNAPSHOT,
      BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE,
      BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE,
      BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE,
      BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE);

  private final TransactionReference takamakaCode;
  private final StorageReference[] accounts;
  private final List<Signer<SignedTransactionRequest<?>>> signers = new ArrayList<>();
  private final String chainId;
  private final long start;  // the time when bids started being placed
  private final Node node;
  private final TransactionReference classpath;
  private final StorageReference auction;
  private final List<BidToReveal> bids = new ArrayList<>();
  private final GasHelper gasHelper;
  private final NonceHelper nonceHelper;

  public static void main(String[] args) throws Exception {
	try (Node node = RemoteNodes.of(new URI(args[0]), 20000)) {
      new Auction(node, Paths.get(args[1]),
        StorageValues.reference(args[2]), args[3],
        StorageValues.reference(args[4]), args[5],
        StorageValues.reference(args[6]), args[7]);
    }
  }

  /**
   * Class used to keep in memory the bids placed by each player,
   * that will be revealed at the end.
   */
  private class BidToReveal {
    private final int player;
    private final BigInteger value;
    private final boolean fake;
    private final byte[] salt;

    private BidToReveal(int player, BigInteger value, boolean fake, byte[] salt) {
      this.player = player;
      this.value = value;
      this.fake = fake;
      this.salt = salt;
    }

    /**
     * Creates in store a revealed bid corresponding to this object.
     * 
     * @return the storage reference to the freshly created revealed bid
     */
    private StorageReference intoBlockchain() throws Exception {
      StorageReference bytes32 = node.addConstructorCallTransaction(TransactionRequests.constructorCall
        (signers.get(player), accounts[player],
        nonceHelper.getNonceOf(accounts[player]), chainId, _500_000,
        panarea(gasHelper.getSafeGasPrice()), classpath, CONSTRUCTOR_BYTES32_SNAPSHOT,
        byteOf(salt[0]), byteOf(salt[1]), byteOf(salt[2]), byteOf(salt[3]),
        byteOf(salt[4]), byteOf(salt[5]), byteOf(salt[6]), byteOf(salt[7]),
        byteOf(salt[8]), byteOf(salt[9]), byteOf(salt[10]), byteOf(salt[11]),
        byteOf(salt[12]), byteOf(salt[13]), byteOf(salt[14]), byteOf(salt[15]),
        byteOf(salt[16]), byteOf(salt[17]), byteOf(salt[18]), byteOf(salt[19]),
        byteOf(salt[20]), byteOf(salt[21]), byteOf(salt[22]), byteOf(salt[23]),
        byteOf(salt[24]), byteOf(salt[25]), byteOf(salt[26]), byteOf(salt[27]),
        byteOf(salt[28]), byteOf(salt[29]), byteOf(salt[30]), byteOf(salt[31])));

      var CONSTRUCTOR_REVEALED_BID
        = ConstructorSignatures.of(
           StorageTypes.classNamed("io.hotmoka.tutorial.examples.auction.BlindAuction$RevealedBid"),
           BIG_INTEGER, BOOLEAN, BYTES32_SNAPSHOT);

      return node.addConstructorCallTransaction(TransactionRequests.constructorCall
        (signers.get(player), accounts[player],
        nonceHelper.getNonceOf(accounts[player]), chainId,
        _500_000, panarea(gasHelper.getSafeGasPrice()), classpath, CONSTRUCTOR_REVEALED_BID,
        StorageValues.bigIntegerOf(value), StorageValues.booleanOf(fake), bytes32));
    }
  }

  private Auction(Node node, Path dir, StorageReference account1, String password1,
      StorageReference account2, String password2, StorageReference account3, String password3)
      throws Exception {

    this.node = node;
    takamakaCode = node.getTakamakaCode();
    accounts = new StorageReference[] { account1, account2, account3 };
    var signature = node.getConfig().getSignatureForRequests();
    Function<? super SignedTransactionRequest<?>, byte[]> toBytes
      = SignedTransactionRequest<?>::toByteArrayWithoutSignature;
    signers.add(signature.getSigner(loadKeys(node, dir, account1, password1).getPrivate(), toBytes));
    signers.add(signature.getSigner(loadKeys(node, dir, account2, password2).getPrivate(), toBytes));
    signers.add(signature.getSigner(loadKeys(node, dir, account3, password3).getPrivate(), toBytes));
    gasHelper = GasHelpers.of(node);
    nonceHelper = NonceHelpers.of(node);
    chainId = node.getConfig().getChainId();
    classpath = installJar();
    auction = createContract();
    start = System.currentTimeMillis();

    StorageReference expectedWinner = placeBids();
    waitUntilEndOfBiddingTime();
    revealBids();
    waitUntilEndOfRevealTime();
    StorageValue winner = askForWinner();

    // show that the contract computes the correct winner
    System.out.println("expected winner: " + expectedWinner);
    System.out.println("actual winner: " + winner);
  }

  private StorageReference createContract() throws Exception {
    System.out.println("Creating contract");

    var CONSTRUCTOR_BLIND_AUCTION = ConstructorSignatures.of(BLIND_AUCTION, INT, INT);

    return node.addConstructorCallTransaction
      (TransactionRequests.constructorCall(signers.get(0), accounts[0],
      nonceHelper.getNonceOf(accounts[0]), chainId, _500_000, panarea(gasHelper.getSafeGasPrice()),
      classpath, CONSTRUCTOR_BLIND_AUCTION,
      StorageValues.intOf(BIDDING_TIME), StorageValues.intOf(REVEAL_TIME)));
  }

  private TransactionReference installJar() throws Exception {
    System.out.println("Installing jar");

    //the path of the user jar to install
    var auctionPath = Paths.get(System.getProperty("user.home")
      + "/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-auction/"
      + Constants.HOTMOKA_VERSION
      + "/io-hotmoka-tutorial-examples-auction-" + Constants.HOTMOKA_VERSION + ".jar");

    return node.addJarStoreTransaction(TransactionRequests.jarStore
      (signers.get(0), // an object that signs with the payer's private key
      accounts[0], // payer
      nonceHelper.getNonceOf(accounts[0]), // payer's nonce
      chainId, // chain identifier
      BigInteger.valueOf(1_000_000), // gas limit: enough for this very small jar
      gasHelper.getSafeGasPrice(), // gas price: at least the current gas price of the network
      takamakaCode, // class path for the execution of the transaction
      Files.readAllBytes(auctionPath), // bytes of the jar to install
      takamakaCode)); // dependency
  }

  private StorageReference placeBids() throws Exception {
    var maxBid = BigInteger.ZERO;
    StorageReference expectedWinner = null;
    var random = new Random();
    var BID = MethodSignatures.ofVoid(BLIND_AUCTION, "bid", BIG_INTEGER, BYTES32_SNAPSHOT);

    int i = 1;
    while (i <= NUM_BIDS) { // generate NUM_BIDS random bids
      System.out.println("Placing bid " + i + "/" + NUM_BIDS);
      int player = 1 + random.nextInt(accounts.length - 1);
      var deposit = BigInteger.valueOf(random.nextInt(1000));
      var value = BigInteger.valueOf(random.nextInt(1000));
      boolean fake = random.nextInt(100) >= 80;
      var salt = new byte[32];
      random.nextBytes(salt); // random 32 bytes of salt for each bid

      // create a Bytes32 hash of the bid in the store of the node
      StorageReference bytes32 = codeAsBytes32(player, value, fake, salt);

      // keep note of the best bid, to verify the result at the end
      if (!fake && deposit.compareTo(value) >= 0)
        if (expectedWinner == null || value.compareTo(maxBid) > 0) {
          maxBid = value;
          expectedWinner = accounts[player];
        }
        else if (value.equals(maxBid))
          // we do not allow ex aequos, since the winner
          // would depend on the fastest player to reveal
          continue;

      // keep the explicit bid in memory, not yet in the node,
      // since it would be visible there
      bids.add(new BidToReveal(player, value, fake, salt));

      // place a hashed bid in the node
      node.addInstanceMethodCallTransaction(TransactionRequests.instanceMethodCall
        (signers.get(player), accounts[player],
        nonceHelper.getNonceOf(accounts[player]), chainId,
        _500_000, panarea(gasHelper.getSafeGasPrice()), classpath, BID,
        auction, StorageValues.bigIntegerOf(deposit), bytes32));

      i++;
    }

    return expectedWinner;
  }

  private void revealBids() throws Exception {
    var REVEAL = MethodSignatures.ofVoid
      (BLIND_AUCTION, "reveal",
       StorageTypes.classNamed("io.hotmoka.tutorial.examples.auction.BlindAuction$RevealedBid"));

    // we create the revealed bids in blockchain; this is safe now, since the bidding time is over
    int counter = 1;
    for (BidToReveal bid: bids) {
      System.out.println("Revealing bid " + counter++ + "/" + bids.size());
      int player = bid.player;
      StorageReference bidInBlockchain = bid.intoBlockchain();
      node.addInstanceMethodCallTransaction(TransactionRequests.instanceMethodCall
        (signers.get(player), accounts[player],
        nonceHelper.getNonceOf(accounts[player]), chainId, _500_000,
        panarea(gasHelper.getSafeGasPrice()),
        classpath, REVEAL, auction, bidInBlockchain));
    }
  }

  private StorageReference askForWinner() throws Exception {
    var AUCTION_END = MethodSignatures.ofNonVoid
      (BLIND_AUCTION, "auctionEnd", PAYABLE_CONTRACT);

    StorageValue winner = node.addInstanceMethodCallTransaction
      (TransactionRequests.instanceMethodCall
      (signers.get(0), accounts[0], nonceHelper.getNonceOf(accounts[0]),
      chainId, _500_000, panarea(gasHelper.getSafeGasPrice()),
      classpath, AUCTION_END, auction)).get();

    // the winner is normally a StorageReference,
    // but it could be a NullValue if all bids were fake
    return winner instanceof StorageReference sr ? sr : null;
  }

  private void waitUntilEndOfBiddingTime() {
    waitUntil(BIDDING_TIME + 5000, "Waiting until the end of the bidding time");
  }

  private void waitUntilEndOfRevealTime() {
    waitUntil(BIDDING_TIME + REVEAL_TIME + 5000, "Waiting until the end of the revealing time");
  }

  /**
   * Waits until a specific time after start.
   */
  private void waitUntil(long duration, String forWhat) {
    long msToWait = start + duration - System.currentTimeMillis();
    System.out.println(forWhat + " (" + msToWait + "ms still missing)");
	try {
      Thread.sleep(start + duration - System.currentTimeMillis());
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Hashes a bid and put it in the store of the node, in hashed form.
   */
  private StorageReference codeAsBytes32(int player, BigInteger value, boolean fake, byte[] salt)
      throws Exception {
	// the hashing algorithm used to hide the bids
	var digest = MessageDigest.getInstance("SHA-256");
    digest.update(value.toByteArray());
    digest.update(fake ? (byte) 0 : (byte) 1);
    digest.update(salt);
    byte[] hash = digest.digest();
    return createBytes32(player, hash);
  }

  /**
   * Creates a Bytes32Snapshot object in the store of the node.
   */
  private StorageReference createBytes32(int player, byte[] hash) throws Exception {
    return node.addConstructorCallTransaction
      (TransactionRequests.constructorCall(
      signers.get(player),
      accounts[player],
      nonceHelper.getNonceOf(accounts[player]), chainId,
      _500_000, panarea(gasHelper.getSafeGasPrice()),
      classpath, CONSTRUCTOR_BYTES32_SNAPSHOT,
      byteOf(hash[0]), byteOf(hash[1]),
      byteOf(hash[2]), byteOf(hash[3]),
      byteOf(hash[4]), byteOf(hash[5]),
      byteOf(hash[6]), byteOf(hash[7]),
      byteOf(hash[8]), byteOf(hash[9]),
      byteOf(hash[10]), byteOf(hash[11]),
      byteOf(hash[12]), byteOf(hash[13]),
      byteOf(hash[14]), byteOf(hash[15]),
      byteOf(hash[16]), byteOf(hash[17]),
      byteOf(hash[18]), byteOf(hash[19]),
      byteOf(hash[20]), byteOf(hash[21]),
      byteOf(hash[22]), byteOf(hash[23]),
      byteOf(hash[24]), byteOf(hash[25]),
      byteOf(hash[26]), byteOf(hash[27]),
      byteOf(hash[28]), byteOf(hash[29]),
      byteOf(hash[30]), byteOf(hash[31])));
  }

  private static KeyPair loadKeys(Node node, Path dir, StorageReference account, String password)
      throws Exception {
    return Accounts.of(account, dir).keys(password,
      SignatureHelpers.of(node).signatureAlgorithmFor(account));
  }
}
```

This test class is relatively long and complex. Let us start from its beginning.
The code specifies that the test will place 10 random bids, that the bidding phase
lasts 100 seconds and that the reveal phase lasts 140 seconds
(these timings are fine on a blockchain that creates a block every four seconds;
shorter block creation times would allow shorter timings):

```java
public final static int NUM_BIDS = 10;
public final static int BIDDING_TIME = 230_000;
public final static int REVEAL_TIME = 350_000;
```

Some constant signatures follow,
that simplify the calls to methods and constructors later.
Method `main()` connects to a remote node and passes it
as a parameter to the constructor of class `Auction`, that
installs `io-hotmoka-tutorial-examples-auction-1.9.0.jar` inside it. It stores the node in field `node`.
Then the constructor of `Auction` creates an `auction` contract in the node
and calls method `placeBids()` that
uses the inner class `BidToReveal` to keep track of the bids placed
during the test, in clear. Initially, bids are kept in
memory, not in the store of the node, where they could be publicly accessed.
Only their hashes are stored in the node.
Method `placeBids()` generates `NUM_BIDS` random bids on behalf
of the `accounts.length -1` players (the first element of the
`accounts` array is the creator of the auction):

```java
int i = 1;
while (i <= NUM_BIDS) {
  int player = 1 + random.nextInt(accounts.length - 1);
  var deposit = BigInteger.valueOf(random.nextInt(1000));
  var value = BigInteger.valueOf(random.nextInt(1000));
  var fake = random.nextInt(100) >= 80; // fake in 20% of the cases
  var salt = new byte[32];
  random.nextBytes(salt);
  ...
}
```

Each random bid is hashed (including a random salt) and a `Bytes32Snapshot` object
is created in the store of the node, containing that hash:

```java
StorageReference bytes32 = codeAsBytes32(player, value, fake, salt);
```

The bid, in clear, is added to a list `bids` that, at the end of the loop,
will contain all bids:

```java
bids.add(new BidToReveal(player, value, fake, salt));
```

The hash is used instead to place a bid in the node:

```java
node.addInstanceMethodCallTransaction(TransactionRequests.instanceMethodCall
  (signers.get(player), accounts[player],
  nonceHelper.getNonceOf(accounts[player]), chainId,
  _500_000, panarea(gasHelper.getSafeGasPrice()), classpath, BID,
  auction, StorageValues.bigIntegerOf(deposit), bytes32));
```

The loop takes also care of keeping track of the best bidder, that placed
the best bid, so that it can be compared at the end with the best bidder
computed by the smart contract (they should coincide):

```java
if (!fake && deposit.compareTo(value) >= 0)
  if (expectedWinner == null || value.compareTo(maxBid) > 0) {
    maxBid = value;
    expectedWinner = accounts[player];
  }
  else if (value.equals(maxBid))
    continue;
```

As you can see, the test above avoids generating a bid that
is equal to the best bid seen so far. This avoids having two bidders
that place the same bid: the smart contract will consider as winner
the first bidder that reveals its bids. To avoid this tricky case, we prefer
to assume that the best bid is unique. This is just a simplification of the
testing code, since the smart contract deals perfectly with that case.

After all bids have been placed, the constructor of `Auction` waits until the end of
the bidding time:

```java
waitUntilEndOfBiddingTime();
```

Then the constructor of `Auction` calls method `revealBids()`, that reveals
the bids to the smart contract, in plain. It creates in the store of the node
a data structure
`RevealedBid` for each elements of the list `bids`, by calling
`bid.intoBlockchain()`.
This creates the bid in clear in the store of the node, but this is safe now,
since the bidding time is over and
they cannot be used to guess a winning bid anymore. Then method `revealBids()`
reveals the bids by calling method `reveal()` of the
smart contract:

```java
for (BidToReveal bid: bids) {
  int player = bid.player;
  StorageReference bidInBlockchain = bid.intoBlockchain();
  node.addInstanceMethodCallTransaction(TransactionRequests.instanceMethodCall
    (signers.get(player), accounts[player],
    nonceHelper.getNonceOf(accounts[player]), chainId, _500_000,
    panarea(gasHelper.getSafeGasPrice()),
    classpath, REVEAL, auction, bidInBlockchain));
}
```

Note that this is possible since the inner class `RevealedBid` of the
smart contract has been annotated as `@Exported`
(see its code in section [A Blind Auction Contract](#a-blind-auction-contract)),
hence its instances can be
passed as argument to calls from outside the blockchain.

Subsequently, the constructor of `Auction` waits until the end of the reveal phase:

```java
waitUntilEndOfRevealTime();
```

After that, method `askForWinner()`
signals to the smart contract that the auction is over and asks about the winner:

```java
StorageValue winner = node.addInstanceMethodCallTransaction
  (TransactionRequests.instanceMethodCall
  (signers.get(0), accounts[0], nonceHelper.getNonceOf(accounts[0]),
  chainId, _500_000, panarea(gasHelper.getSafeGasPrice()),
  classpath, AUCTION_END, auction)).get();
```

The final two `System.out.println()`'s in the constructor of `Auction`
allow one to verify that the smart contract
actually computes the right winner, since they will always print the identical storage
object (different at each run, in general), such as:

```
expected winner: 2c9eddf45836994e81a8bc90bdfa04d38d823776ce25b40ef96feab7f9e14a42#0
actual winner: 2c9eddf45836994e81a8bc90bdfa04d38d823776ce25b40ef96feab7f9e14a42#0
```

We can run class `Auction` now (please note that the execution of this test will take a few minutes):

```shell
mvn compile exec:java -Dexec.mainClass="io.hotmoka.tutorial.examples.runs.Auction"
  -Dexec.args="ws://panarea.hotmoka.io:8002
   hotmoka_tutorial
   9149d644f3c98ea8e6747690b7787f03e83c185f48b815570843a547eb5a3efd#0 banana
   2c9eddf45836994e81a8bc90bdfa04d38d823776ce25b40ef96feab7f9e14a42#0 mango
   073c69c124364d5ddc2cfc3c0dd581dcaa63c2d1bb435ad3c40f78de84b2455e#0 strawberry"
```

Its execution should print something like this on the console:

```
Installing jar
Creating contract
Placing bid 1/10
Placing bid 2/10
Placing bid 3/10
Placing bid 4/10
Placing bid 5/10
Placing bid 6/10
Placing bid 7/10
Placing bid 8/10
Placing bid 9/10
Placing bid 10/10
Waiting until the end of the bidding time (32964ms still missing)
Revealing bid 1/10
Revealing bid 2/10
Revealing bid 3/10
Revealing bid 4/10
Revealing bid 5/10
Revealing bid 6/10
Revealing bid 7/10
Revealing bid 8/10
Revealing bid 9/10
Revealing bid 10/10
Waiting until the end of the revealing time (49548ms still missing)
expected winner: 2c9eddf45836994e81a8bc90bdfa04d38d823776ce25b40ef96feab7f9e14a42#0
actual winner: 2c9eddf45836994e81a8bc90bdfa04d38d823776ce25b40ef96feab7f9e14a42#0
```

### Listening to Events

__[See `io-hotmoka-tutorial-examples-runs` in `https://github.com/Hotmoka/hotmoka`]__

The `BlindAuction` contract generates events during its execution. If an external tool, such
as a wallet, wants to listen to such events and trigger some activity when they occur,
it is enough for it to subscribe to the events of a node that is executing the contract,
by providing a handler that gets executed each time a new event gets generated.
Subscription requires to specify the creator of the events that should be forwarded to the
handler. In our case, this is the `auction` contract. Thus, clone the `Auction.java` class into
`Events.java` and modify its constructor as follows:

```java
...
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.UnknownReferenceException;
...
auction = createAuction();
start = System.currentTimeMillis();

try (var subscription = node.subscribeToEvents(auction, this::eventHandler)) {
  StorageReference expectedWinner = placeBids();
  waitUntilEndOfBiddingTime();
  revealBids();
  waitUntilEndOfRevealTime();
  StorageValue winner = askForWinner();

  System.out.println("expected winner: " + expectedWinner);
  System.out.println("actual winner: " + winner);

  waitUntilAllEventsAreFlushed();
}

private void waitUntilAllEventsAreFlushed() {
  waitUntil(BIDDING_TIME + REVEAL_TIME + 12000, "Waiting until all events are flushed");
}

private void eventHandler(StorageReference creator, StorageReference event) {
  try {
    System.out.println
      ("Seen event of class " + node.getClassTag(event).getClazz()
       + " created by contract " + creator);
  }
  catch (ClosedNodeException | UnknownReferenceException | TimeoutException e) {
    System.out.println("The node is misbehaving: " + e.getMessage());
  }
  catch (InterruptedException e) {
    Thread.currentThread().interrupt();
  }
}
...
```

The event handler, in this case, simply prints on the console the class of the event and its creator
(that will coincide with `auction`). You can run the `Events` class now:

```shell
mvn compile exec:java -Dexec.mainClass="io.hotmoka.tutorial.examples.runs.Events"
  -Dexec.args="ws://panarea.hotmoka.io:8002
   hotmoka_tutorial
   9149d644f3c98ea8e6747690b7787f03e83c185f48b815570843a547eb5a3efd#0 banana
   2c9eddf45836994e81a8bc90bdfa04d38d823776ce25b40ef96feab7f9e14a42#0 mango
   073c69c124364d5ddc2cfc3c0dd581dcaa63c2d1bb435ad3c40f78de84b2455e#0 strawberry"
```
You should see something like this on the console:

```
Installing jar
Creating contract
Placing bid 1/10
Placing bid 2/10
Placing bid 3/10
Placing bid 4/10
Placing bid 5/10
Placing bid 6/10
Placing bid 7/10
Placing bid 8/10
Placing bid 9/10
Placing bid 10/10
Waiting until the end of the bidding time (33068ms still missing)
Revealing bid 1/10
Revealing bid 2/10
Revealing bid 3/10
Seen event of class io.hotmoka.tutorial.examples.auction.BidIncrease created 
  by contract 92b643b21a6694a4b31b44ce6d073ffcae24cd2779b0f8669bf182546af2b79f#0
Revealing bid 4/10
Revealing bid 5/10
Revealing bid 6/10
Revealing bid 7/10
Revealing bid 8/10
Revealing bid 9/10
Revealing bid 10/10
Waiting until the end of the revealing time (49550ms still missing)
expected winner: 073c69c124364d5ddc2cfc3c0dd581dcaa63c2d1bb435ad3c40f78de84b2455e#0
actual winner: 073c69c124364d5ddc2cfc3c0dd581dcaa63c2d1bb435ad3c40f78de84b2455e#0
Waiting until all events are flushed (6167ms still missing)
Seen event of class io.hotmoka.tutorial.examples.auction.AuctionEnd created 
  by contract 92b643b21a6694a4b31b44ce6d073ffcae24cd2779b0f8669bf182546af2b79f#0
```

> The `subscribeToEvents()` method returns a `Subscription` object that should be
> closed when it is not needed anymore, in order to reduce the overhead on the node.
> Since it is an `AutoCloseable` resource, the recommended technique is to use a
> try-with-resource construct, as shown in the previous example.
> Moreover, our code waits for a few seconds before closing the
> subscription to the events, in order to give events the time to be forwarded to the client.

In general, event handlers can perform arbitrarily complex operations and even access the
event object in the store of the node,
from its storage reference, reading its fields or calling its methods. Please remember, however,
that event handlers are run in a thread of the node. Hence, they should be fast and shouldn't hang.
It is good practice to let event handlers add events in a queue, in a non-blocking way.
A consumer thread, external to the node, then retrieves the events from the queue and processes them in turn.

It is possible to subscribe to _all_ events generated by a node,
by using `null` as creator in the `subscribeToEvents()` method. Think twice before doing that,
since your handler will be notified of _all_ events generated by _any_ application installed in
the node. It might be a lot.

# Tokens

A popular class of smart contracts
implement a dynamic ledger of coin transfers between accounts. These
coins are not native tokens, but rather new, derived tokens.
In some sense, tokens are programmed money, whose rules are specified
by a smart contract and enforced by the underlying blockchain.

> In this context, the term _token_ is used
> for the smart contract that tracks coin transfers, for the single coin units and for the category
> of similar coins. This is sometimes confusing.

Native and derived tokens can be categorized in many
ways [[OliveiraZBS18](#references),[Freni20](#references),[Tapscott20](#references)].
The most popular classification
is between _fungible_ and _non-fungible_ tokens.
Fungible tokens are interchangeable with each other, since they have an identical
nominal value that does not depend on each specific token instance.
Native tokens and traditional (_fiat_) currencies are both examples of fungible tokens.
Their main application is in the area of crowdfunding and initial coin offers
to support startups.
On the contrary, non-fungible tokens have a value that depends on their specific instance.
Hence, in general, they are not interchangeable.
Their main application is currently in the art market, where they represent
a written declaration of author's rights concession to the holder.

A few standards have emerged for such tokens,
that should guarantee correctness,
accessibility, interoperability, management and security
of the smart contracts that run the tokens.
Among them, the Ethereum Requests for Comment \#20
(ERC-20, see [https://eips.ethereum.org/EIPS/eip-20](https://eips.ethereum.org/EIPS/eip-20))
and \#721
(ERC-721, see [https://eips.ethereum.org/EIPS/eip-721](https://eips.ethereum.org/EIPS/eip-721))
are the most popular, also outside Ethereum.
They provide developers with
a list of rules required for the correct integration of tokens
with other smart contracts and with applications external to the blockchain,
such as wallets, block explorers, decentralized finance protocols and games.

The most popular implementations of the ERC-20 and ERC-721 standards are in Solidity,
by OpenZeppelin
(see [https://docs.openzeppelin.com/contracts/2.x/erc20](https://docs.openzeppelin.com/contracts/2.x/erc20)
and [https://docs.openzeppelin.com/contracts/2.x/erc721](https://docs.openzeppelin.com/contracts/2.x/erc721)),
a team of programmers in the Ethereum community
who deliver useful and secure smart contracts and libraries, and by
ConsenSys, later deprecated in favor of OpenZeppelin's.
OpenZeppelin extends ERC-20 with snapshots, that is,
immutable views of the state of a token contract, that show
its ledger at a specific instant of time.
They are useful to investigate the consequences of an attack, to create forks of the token
and to implement mechanisms based on token balances such as weighted voting.

## Fungible Tokens (ERC20)

A fungible token ledger is a ledger that binds owners (contracts) to
the numerical amount of tokens they own. With this very high-level description,
it is an instance of the `IERC20View` interface in Figure 32.
The `balanceOf` method tells how many tokens an `account` holds and the method
`totalSupply` provides the total number of tokens in circulation.
The `UnsignedBigInteger` class is a Takamaka library class that wraps a `BigInteger`
and guarantees that its value is never negative. For instance, the subtraction of two
`UnsignedBigInteger`s throws an exception when the second is larger than the first.

 <p align="center"><img width="800" src="pics/erc20.png" alt="Figure 32. The hierarchy of the ERC20 token implementations"></p><p align="center">Figure 32. The hierarchy of the ERC20 token implementations</p>


The `snapshot` method, as already seen for collection classes, yields a read-only,
frozen view of the latest state of the token ledger.
Since it is defined in the topmost interface, all token classes
can be snapshotted. Snapshots are computable in constant time.

> In the original ERC20 standard and implementation in Ethereum,
> only specific subclasses allow snapshots, since their creation adds gas costs to all
> operations, also for token owners that never performed any snapshot.
> See the discussion and comparison in [[CrosaraOST21]](#references).

An ERC20 ledger is typically modifiable. Namely, owners
can sell tokens to other owners
and can delegate trusted contracts to transfer tokens on their behalf.
Of course, these operations must be legal, in the sense that an owner cannot sell
more tokens than it owns and delegated contracts cannot transfer more tokens than the
cap to their delegation.
These modification operations are defined in the
`IERC20` interface in Figure 32. They are identical to the same
operations in the ERC20 standard for Ethereum, hence we refer to that standard for further detail.
The `view()` method is used to yield a _view_ of the ledger, that is, an object
that reflects the current state of the original ledger, but without any modification operation.

The `ERC20` implementation provides a standard implementation for the functions defined
in the `IERC20View` and `IERC20` interfaces. Moreover, it provides metadata information
such as name, symbol and number of decimals for the specific token implementation.
There are protected implementations for methods that allow one to mint or burn an amount
of tokens for a given owner (`account`). These are protected since one does not
want to allow everybody to print or burn money. Instead, subclasses can call into these
methods in their constructor, to implement an initial distribution of tokens,
and can also allow subsequent, controlled mint or burns.
For instance, the `ERC20Burnable` class is an `ERC20` implementation that
allows a token owner to burn its tokens only, or those it has been
delegated to transfer, but never those of another owner.

The `ERC20Capped` implementation allows the specification of a maximal cap to the
number of tokens in circulation. When new tokens get minted, it checks that the cap
is not exceeded and throws an exception otherwise.

### Implementing Our Own ERC20 Token

__[See `io-hotmoka-tutorial-examples-erc20` in `https://github.com/Hotmoka/hotmoka`]__

Let us define a token ledger class that allows only its creator the mint or burn tokens.
We will call it `CryptoBuddy`. As Figure 32 shows,
we plug it below the `ERC20` implementation, so that we inherit that implementation
and do not need to reimplement the methods of the `ERC20` interface.

Create in Eclipse a new Maven Java 21 (or later) project named `erc20`.
You can do this by duplicating the project `io-hotmoka-tutorial-examples-family`.
Use the following `pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                        http://maven.apache.org/xsd/maven-4.0.0.xsd">

  <modelVersion>4.0.0</modelVersion>
  <groupId>io.hotmoka</groupId>
  <artifactId>io-hotmoka-tutorial-examples-erc20</artifactId>
  <version>1.9.0</version>

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.release>21</maven.compiler.release>
  </properties>

  <dependencies>
    <dependency>
      <groupId>io.hotmoka</groupId>
      <artifactId>io-takamaka-code</artifactId>
      <version>1.5.0</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.11.0</version>
      </plugin>
    </plugins>
  </build>

</project>
```

and the following `module-info.java`:

```java
module erc20 {
  requires io.takamaka.code;
}
```

Create package `io.hotmoka.tutorial.examples.erc20` inside `src/main/java` and add
the following `CryptoBuddy.java` inside that package:

```java
package io.hotmoka.tutorial.examples.erc20;

import static io.takamaka.code.lang.Takamaka.require;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.math.UnsignedBigInteger;
import io.takamaka.code.tokens.ERC20;

public class CryptoBuddy extends ERC20 {
  private final Contract owner;

  public @FromContract CryptoBuddy() {
    super("CryptoBuddy", "CB");
    owner = caller();
    var initialSupply = new UnsignedBigInteger("200000");
    var multiplier = new UnsignedBigInteger("10").pow(18);
    _mint(caller(), initialSupply.multiply(multiplier)); // 200'000 * 10 ^ 18
  }

  public @FromContract void mint(Contract account, UnsignedBigInteger amount) {
    require(caller() == owner, "Lack of permission");
    _mint(account, amount);
  }

  public @FromContract void burn(Contract account, UnsignedBigInteger amount) {
    require(caller() == owner, "Lack of permission");
    _burn(account, amount);
  }
}
```

The constructor of `CryptoBuddy` initializes the total supply by minting
a very large number of tokens. They are initially owned by the creator of the contract,
that is saved as `owner`. Methods `mint` and `burn` check that the owner is requesting
the mint or burn and call the inherited protected methods in that case.

You can generate the jar archive:

```shell
$ cd io-hotmoka-tutorial-examples-erc20
$ mvn install
```

Then you can install that jar in the node, by letting our first account pay:

```shell
$ cd ..
$ moka jars install cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
    io-hotmoka-tutorial-examples-erc20/target/io-hotmoka-tutorial-examples-erc20-1.9.0.jar
    --password-of-payer
    --uri ws://panarea.hotmoka.io:8001

Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate
Do you really want to install the jar spending up to 830600 gas units
  at the price of 1 pana per unit (that is, up to 830600 panas) [Y/N] Y
Adding transaction d77db2757d6e4d1e9624c7ceeb6726b9e763755d4824ed52a60561a86f6642fb... done.
The jar has been installed at d77db2757d6e4d1e9624c7ceeb6726b9e763755d4824ed52a60561a86f6642fb.

Gas consumption:
 * total: 9844
   * for CPU: 1619
   * for RAM: 3314
   * for storage: 4911
   * for penalty: 0
 * price per unit: 1 pana
 * total price: 9844 panas
```

Finally, you can create an instance of the token class, by always letting our first account pay for that:

```shell
$ moka objects create cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#0
   io.hotmoka.tutorial.examples.erc20.CryptoBuddy
   --classpath d77db2757d6e4d1e9624c7ceeb6726b9e763755d4824ed52a60561a86f6642fb
   --uri ws://panarea.hotmoka.io:8001 --password-of-payer

Enter value for --password-of-payer (the password of the key pair of the payer account): chocolate
Do you really want to call constructor public ...CryptoBuddy() spending up to 200000 gas units
  at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y
Adding transaction 766f07dabd946d133a7ffd14c7ba8b0aa6bf77480c456968faad4c345fd694e4... done.
A new object 766f07dabd946d133a7ffd14c7ba8b0aa6bf77480c456968faad4c345fd694e4#0 has been created.

Gas consumption:
 * total: 9955
   * for CPU: 3377
   * for RAM: 5735
   * for storage: 843
   * for penalty: 0
 * price per unit: 1 pana
 * total price: 9955 panas
```

The new ledger instance is installed in the storage of the node now, at the address
`766f07dabd946d133a7ffd14c7ba8b0aa6bf77480c456968faad4c345fd694e4#0`. It is possible to start interacting with that ledger instance, by transferring
tokens between accounts. For instance, this can be done with the `moka objects call` command,
that allows one to invoke the `transfer` or `transferFrom` methods of the ledger.
It is possible to show the state of the ledger with the `moka objects show` command, although specific
utilities will provide a more user-friendly view of the ledger in the future.

## Richer than Expected

Every owner of ERC20 tokens can decide to send some of its tokens to another
contract _C_, that will become an owner itself, if it was not already.
This means that the ledger inside
an `ERC20` implementation gets modified and some tokens get registered for
the new owner _C_. However, _C_ is not notified in any way of this transfer.
This means that our contracts could be richer than we expect, if somebody
has sent tokens to them, possibly inadvertently. In theory, we could
scan the whole memory of a Hotmoka node, looking for implementations
of the `IERC20` interface, and check if our contracts are registered inside them.
Needless to say, this is computationally irrealistic.
Moreover, even if we know that one of our contracts is waiting to receive
some tokens, we don't know immediately when this happens, since
the contract does not get notified of any transfer of tokens.

This issue is inherent to the definition of the ERC20 standard in Ethereum
and the implementation in Takamaka inherits this limitation, since it wants to stick
as much as possible to the Ethereum standard. A solution to the problem
would be to restrict the kind of owners that are allowed in Figure 32.
Namely, instead of allowing all `Contract`s, the signature of the methods could
be restricted to owners of some interface type `IERC20Receiver`, with a single method
`onReceive` that gets called by the `ERC20` implementation, every time
tokens get transferred to an `IERC20Receiver`.
In this way, owners of ERC20 tokens get notified when they receive new tokens.
This solution has never been implemented for ERC20 tokens in Ethereum, while
it has been used in the ERC721 standard for non-fungible tokens, as we will
show in the next section.

## Non-Fungible Tokens (ERC721)

A non-fungible token is implemented as a ledger that maps each token identifier to its owner.
Ethereum provides the ERC721 specification for non-fungible tokens.
There, a token identifier is an array of bytes. Takamaka uses, more generically,
a `BigInteger`. Note that a `BigInteger` can be constructed from an array of bytes
by using the constructor of class `BigInteger` that receives an array of bytes.
In the ERC721 specification, token owners are contracts, although the implementation will check
that only contracts implementing the `IERC721Receiver` interface are added
to an `IERC721` ledger, or externally owned accounts.

> The reason for allowing externally owned accounts is probably a simplification,
> since Ethereum users own externally owned accounts and it is simpler for them
> to use such accounts directly inside an ERC721 ledger, instead of creating
> contracts of type `IERC721Receiver`. In any case, no other kind of contracts
> is allowed in ERC721 implementations.

The hierarchy of the Takamaka classes for the ERC721 standard is shown
in Figure 33.

 <p align="center"><img width="800" src="pics/erc721.png" alt="Figure 33. The hierarchy of the ERC721 token implementations"></p>


As in the case of the ERC20 tokens, the interface `IERC721View` contains
the read-only operations that implement a ledger of non-fungible
tokens: the `ownerOf` method yields the owner of a given token and
the `balanceOf` method returns the number of tokens held by a given `account`.
The `snapshot()` method yields a frozen, read-only view of the latest state of the ledger.

The `IERC721` subinterface adds methods for token transfers.
Please refer to their description given by the Ethereum standard.
We just say here that
the `transferFrom` method moves a given token from its previous
owner to a new owner. The caller of this method can be the owner of the
token, but it can also be another contract, called _operator_,
as long as the latter has
been previously approved by the token owner, by using the
`approve` method. It is also possible to approve an operator for all
one's tokens (or remove such approval), through the `setApprovalForAll` method.
The `getApproved` method tells who is the operator approved for a given token
(if any) and the `isApprovedForAll` method tells if a given operator has been
approved to transfer all tokens of a given `owner`. The `view` method
yields a read-only view of the ledger, that reflects all future changes to the ledger.

The implementation `ERC721` provides standard implementations for all methods
of `IERC721View` and `IERC721`, adding metadata information about the name and the
symbol of the token and protected methods for minting and burning
new tokens. These are meant to be called in subclasses, such as
`ERC721Burnable`. Namely, the latter adds a `burn` method that allows the owner of a token
(or its approved operator) to burn the token.

As we have already said previously, the owners of the tokens are declared as contracts
in the `IERC721View` and `IERC721` interfaces, but the `ERC721` implementation
actually requires them to be `IERC71Receiver`s or externally owned accounts.
Otherwise, the methods of `ERC721` will throw an exception.
Moreover, token owners that implement the `IERC721Receiver` interface
get their `onReceive` method called whenever new tokens are transferred to them.

> The ERC721 standard requires `onReceive` to return a special message, in order
> to prove that the contract actually executed that method. This is a very technical
> necessity of Solidity, whose first versions allowed one to call non-existent methods
> without getting an error. It is a sort of security measure, since Solidity
> has no `instanceof` operator and cannot check in any reliable
> way that the token owners are actually instances of the interface `IERC721Receiver`.
> The implementation in Solidity uses the ERC165 standard for interface detection,
> but that standard is not a reliable replacement of `instanceof`,
> since a contract can always pretend to belong to any contract type.
> Takamaka is Java and can use the `instanceof` operator, that works correctly.
> As a consequence, the `onReceive` method in Takamaka needn't return any value.

### Implementing Our Own ERC721 Token

__[See `io-hotmoka-tutorial-examples-erc721` in `https://github.com/Hotmoka/hotmoka`]__

Let us define a ledger for non-fungible tokens
that only allows its creator the mint or burn tokens.
We will call it `CryptoShark`. As Figure 33 shows,
we plug it below the `ERC721` implementation, so that we inherit that implementation
and do not need to reimplement the methods of the `ERC721` interface.
The code is almost identical to that for the `CryptoBuddy` token defined
in [Implementing Our Own ERC20 Token](#implementing-our-own-erc20-token).

Create in Eclipse a new Maven Java 21 (or later) project named `erc721`.
You can do this by duplicating the project `io-hotmoka-tutorial-examples-erc20`.
Use the following `pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                        http://maven.apache.org/xsd/maven-4.0.0.xsd">

  <modelVersion>4.0.0</modelVersion>
  <groupId>io.hotmoka</groupId>
  <artifactId>io-hotmoka-tutorial-examples-erc721</artifactId>
  <version>1.9.0</version>

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.release>21</maven.compiler.release>
  </properties>

  <dependencies>
    <dependency>
      <groupId>io.hotmoka</groupId>
      <artifactId>io-takamaka-code</artifactId>
      <version>1.5.0</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.11.0</version>
      </plugin>
    </plugins>
  </build>

</project>
```

and the following `module-info.java`:

```java
module erc721 {
  requires io.takamaka.code;
}
```

Create package `io.hotmoka.tutorial.examples.erc721` inside `src/main/java` and add
the following `CryptoShark.java` inside that package:

```java
package io.hotmoka.tutorial.examples.erc721;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.tokens.ERC721;

public class CryptoShark extends ERC721 {
  private final Contract owner;

  public @FromContract CryptoShark() {
    super("CryptoShark", "SHK");
    owner = caller();
  }

  public @FromContract void mint(Contract account, BigInteger tokenId) {
    require(caller() == owner, "Lack of permission");
    _mint(account, tokenId);
  }

  public @FromContract void burn(BigInteger tokenId) {
    require(caller() == owner, "Lack of permission");
    _burn(tokenId);
  }
}
```

The constructor of `CryptoShark` takes note of the creator of the token.
That creator is the only that is allowed to mint or burn tokens, as
you can see in methods `mint` and `burn`.

You can compile the file:

```shell
$ cd io-hotmoka-tutorial-examples-erc721
$ mvn install
```

Then you can install that jar in the node and create an instance of the token
exactly as we did for the `CryptoBuddy` ERC20 token before.

# Hotmoka Nodes

A Hotmoka node is a device that implements an interface for running Java code
remotely. It can be any kind of device, such as a device of an IoT network,
but also a node of a blockchain. We have already used instances of Hotmoka nodes,
namely, instances of `RemoteNode`. But there are other examples of nodes, that we
will describe in this chapter.

The interface `io.hotmoka.node.api.Node` is shown in the topmost part of Figure 34.
That interface can be split in five parts:

1. A `get` part, that includes methods for querying the
   state of the node and for accessing the objects contained in its store.
2. An `add` part, that expands the store of the node with the result of a transaction.
3. A `run` part, that runs transactions that execute `@View` methods and hence do not
   expand the store of the node.
4. A `post` part, that expands the store of the node with the result of a transaction,
   without waiting for its result; instead, a future is returned.
5. A `contextual` part, that allows users to subscribe listeners of events generated during
   the execution of the transactions, or listeners called when the node gets closed, or
   to close the node itself.

 <p align="center"><img width="800" src="pics/nodes.png" alt="Figure 34. The hierarchy of Hotmoka nodes"></p><p align="center">Figure 34. The hierarchy of Hotmoka nodes.</p>


If a node belongs to a blockchain, then all nodes of the blockchain have the same vision
of the state, so that it is equivalent to call a method on a node or on any other node of the
network. The only methods that are out of consensus, since they deal with information specific
to each node, are `getInfo` and the four contextual methods
`subscribeToEvents`, `addOnCloseHandler`, `removeOnCloseHandler` and `close`.

Looking at Figure 34, it is possible to see that
the `Node` interface has many implementations, that we describe below.

#### Local Implementations

*Local Implementations* are actual nodes that run on the machine
where they have been started. For instance, they can be a node
of a larger blockchain network. Among them,
`TendermintNode` implements a node of a Tendermint blockchain
and will be presented in [Tendermint Nodes](#tendermint-nodes).
`DiskNode` implements a single-node blockchain in disk memory.
It is useful for debugging, testing and learning, since it allows
one to inspect the content of blocks, transactions and store.
It will be presented in [Disk Nodes](#disk-nodes).
Local nodes can be instantiated through the static
factory method `init()` of their supplier class. That method requires to specify
parameters that are global to the network of nodes (`ConsensusConfig`) and must be the same
for all nodes in the network, and parameters that are specific to the given node
of the network that is being started
and can be different from node to node (`TendermintNodeConfig` and similar).
Some implementations have to ability to _resume_.
This means that they recover the state at the end of a previous execution, reconstruct the
consensus parameters from that state and resume the execution from there, downloading
and verifying blocks already processed by the network. In order to resume
a node from an already existing state, the static `resume()` method of its supplier class
must be used.

#### Decorators

The `Node` interface is implemented by some decorators as well.
Typically, these decorators run some transactions on the decorated node,
to simplify some tasks, such as the initialization of the node, the installation of jars into the node
or the creation of accounts in the node. These decorators are views of the decorated node, in the sense
that any method of the `Node` interface, invoked on the decorator, is forwarded
to the decorated node, with the exception of the contextual methods that are executed locally
on the specific node where they are invoked.
We will discuss them in [Node Decorators](#node-decorators).

#### Adaptors

Very often, one wants to _publish_ a node online,
so that we (and other programmers who need its service) can use it concurrently.
This should be possible for all implementations of the `Node` interface,
such as `DiskNode`, `TendermintNode` and all present and future implementations.
In other words, we would like to publish _any_
Hotmoka node as a service, accessible through the internet. This will be the subject
of [Hotmoka Services](#hotmoka-services).
Conversely, once a Hotmoka node has been published at some URI, say
`ws://my.company.com`, it will be accessible through websockets. This complexity
might make it awkward, for a programmer, to use the published node.
In that case, we can create an instance of `Node` that operates as
a proxy to the network service, helping programmers integrate
their software to the service in a seamless way. This _remote_ node still implements
the `Node` interface, but simply forwards all its calls to the remote service
(with the exception of the contextual methods, that are executed locally on
the remote node itself). By programming against
the same `Node` interface, it becomes easy for a programmer
to swap a local node with a remote node, or
vice versa. This mechanism is described in
[Remote Nodes](#remote-nodes),
where the adaptor interface `RemoteNode` in Figure 34 is presented.

## Tendermint Nodes

Tendermint [[Tendermint]](#references) is a
Byzantine-fault tolerant engine for building blockchains, that
replicates a finite-state machine on a network of nodes across the world.
The finite-state machine is often referred to as a *Tendermint app*.
The nice feature of Tendermint is that it takes care of all
issues related to networking and consensus, leaving to the
developer only the task to develop the Tendermint app.

 <p align="center"><img width="700" src="pics/hotmoka_tendermint.png" alt="Figure 35. The architecture of the Hotmoka node based on Tendermint"></p><p align="center">Figure 35. The architecture of the Hotmoka node based on Tendermint.</p>


There is a Hotmoka node that implements such a Tendermint app,
for programming in Takamaka over Tendermint. We have already used that node
in the previous chapter, since that installed at
`ws://panarea.hotmoka.io:8002` is a node of that type.
Figure 35
shows the architecture of a Tendermint Hotmoka node.
It consists of a few components.
The Hotmoka component is the Tendermint app that
implements the transactions on the state, resulting from the installation
of jars and the execution of code written in the Takamaka subset of Java. This part is the same in every
implementation of a Hotmoka node, not only for this one based on Tendermint.
In most cases, as here, the database that contains the state is implemented by
using the Xodus transactional database by IntelliJ.
What is specific here, however, is that transactions are put inside a blockchain
implemented by Tendermint. The communication occurs, internally, through the two TCP ports
26657 and 26658, that are the standard choice of Tendermint for communicating with an app.
Clients can contact the Hotmoka node
through any port, typically but not exclusively 8001 or 8002,
as a service that implements the interface `Node` in Figure 34.
The node can live alone but is normally integrated with other Hotmoka nodes based on Tendermint, so that
they execute and verify the same transactions, reaching the same state at the end. This happens through
the TCP port 26656, that allows Tendermint instances to _gossip_: they exchange transactions and information on peers
and finally reach consensus.
Each node can be configured to use a different port to communicate with clients,
which is useful if, for instance, ports 8001 or 8002 (or both)
are already used by some other service.
Port 26656 must be the same for all nodes in the network, since they must communicate on
a standard port.

We can use `ws://panarea.hotmoka.io:8002` to play with
accounts and Takamaka contracts. However, we might want to
install our own node, part of the same blockchain network of `ws://panarea.hotmoka.io:8002`
or part of a brand new blockchain. In the former case, our own node will execute
the transactions, exactly as `ws://panarea.hotmoka.io:8002`, so that we can be sure that they are
executed according to the rules. In the latter case, we can have our own blockchain that
executes our transactions only, instead of using a shared blockchain such as that
at `ws://panarea.hotmoka.io:8002`.

This section shows how you can start your own Hotmoka Tendermint node,
consisting of a single validator node, hence part of its own blockchain.
The process is not difficult but is a bit tedious,
because it requires one to install Tendermint and to create
its configuration files. Section [Starting a Tendermint Node with Docker](#starting-a-tendermint-hotmoka-node-with-docker)
in the next chapter
provides a simpler alternative for reaching the same goal, by using the Docker tool.

> We strongly suggest you to use Docker to install Hotmoka nodes, instead of the instructions
> in this section, hence please
> follow the instructions in [Starting a Tendermint Node with Docker](#starting-a-tendermint-hotmoka-node-with-docker).
> The current section only exists in order to understand what happens inside the Docker container.
> If you are not a developer, or if you are not interested in the topic, please skip this section and
> jump directly to the next chapter.

In order to use a Tendermint Hotmoka node, the Tendermint executable must be
installed in our machine, or our experiments will fail. The Hotmoka node
works with Tendermint version 0.34.15, that can be downloaded in executable
form from [https://github.com/tendermint/tendermint/releases/tag/v0.34.15](https://github.com/tendermint/tendermint/releases/tag/v0.34.15).
Be sure that you download the executable for the architecture of your computer
and install it at a place that is
part of the command-line path of your computer. This means that,
if you run the following command in a shell:

```shell
$ tendermint version
```

the answer must be

```
0.34.15
```

or similar, as long as the version starts with 0.34.15. Our Hotmoka node built on Tendermint is known
to work on Windows, MacOs and Linux machines.

Before starting a local node of a Hotmoka blockchain based on Tendermint, you
need to create the Tendermint configuration file. For instance, in order
to run a single validator node with no non-validator nodes, you can create
its configuration files as follows:

```shell
$ tendermint testnet --v 1 --n 0
```

This will create a directory `mytestnet/node0`
for a single Tendermint node, that includes the configuration
of the node and its private and public validator keys.

Once this is done, you can create a key pair for the gamete
of the node that you are going to start. You perform this with `moka`:

```shell
$ moka keys create --name gamete.pem --password
Enter value for --password (the password that will be needed later to use the key pair): mypassword
The new key pair has been written into "gamete.pem":
* public key: 677QPRLfS4Mwgy2xA4dSEWmM3Hyb43mdZedSzq2g8Yxt (ed25519, base58)
* public key: S9so3T9QIAau/zTpsAlKEhkkJOsqV3HDIQCc72ITha0= (ed25519, base64)
* Tendermint-like address: 8380F3BEC1568BC3A07DE0CA721BA91CD9BC5282
```

The key pair is represented as a `gamete.pem` that contains its _entropy_.
While the entropy and the password are secret information
that you should not distribute, the public key can be used to create a new node.
Namely, you can start a Hotmoka node based on Tendermint,
that uses the Tendermint configuration
directory that you have just created, and with a gamete controlled
by the `gamete.pem` key pair,
by using the `moka init-tendermint` command. You also need
to specify the jar of the runtime of Takamaka, that will
be stored inside the node as `takamakaCode`: we use the
local Maven's cache for that but you can alternatively download the
`io.takamaka-code-1.5.0.jar` file from Maven and refer to
it in the following command line:

```shell
$ moka nodes tendermint init ~/.m2/repository/io/hotmoka/io-takamaka-code/1.5.0/io-takamaka-code-1.5.0.jar
    --public-key-of-gamete=677QPRLfS4Mwgy2xA4dSEWmM3Hyb43mdZedSzq2g8Yxt
    --tendermint-config=mytestnet/node0
Do you really want to start a new node at "chain" (old blocks and store will be lost) [Y/N] Y
The following service has been published:
 * ws://localhost:8001: the API of this Hotmoka node

The validators are the following accounts:
 * e8d06a563da12615a39baecf6bb041a9c39d795be094cc5803971f66cf54ecaf#0
     with public key A5bJbdZWudJv4JxVAmi4QDyDwAyJYc8xxcP1dEdc1Tsh (ed25519, base58)

The owner of the key pair of the gamete can bind it now to its address with:
  moka keys bind file_containing_the_key_pair_of_the_gamete --password --url url_of_this_Hotmoka_node
or with:
  moka keys bind file_containing_the_key_pair_of_the_gamete --password
    --reference 686601110429b794e762d7013bad9012289cab0204c357c916db4c8f654aca57#0

Press the enter key to stop this process and close this node: 
```

This command has done a lot! It has created an instance
of `TendermintNode`; it has stored the `io-takamaka-code-1.5.0.jar` jar
inside it; it has created
a Java object, called manifest, that contains other objects, including
an externally-owned account named `gamete`, whose public key is
that provided after `--public-key-of-gamete`;
it has initialized the balance of the gamete to
the a default value, that can be overriden with the option `--initial-supply`. Finally, this command
has published an internet service at the URI `ws://localhost:8001`,
reachable through websockets connections, that exports the API
of the node.

> By default, `moka nodes tendermint init` publishes the service at port 8001. This can be changed
> with its `--port` option.

> The chain identifier of the blockchain is specified inside the Tendermint configuration
> files. You can edit such files and set your preferred chain identifier before invoking
> `moka nodes tendermint init`.

In order to use the gamete, you should bind its key to its actual storage
reference in the node, on your local machine. Open another shell,
move inside the directory holding the keys of the gamete and digit:

```shell
$ moka keys bind gamete.pem --password
Enter value for --password (the password of the key pair): mypassword
The key pair of 686601110429b794e762d7013bad9012289cab0204c357c916db4c8f654aca57#0
  has been saved
  as "686601110429b794e762d7013bad9012289cab0204c357c916db4c8f654aca57#0.pem".
```

This operation has created a pem file whose name is that of the storage reference of the gamete.
With this file, it is possible to run transactions on behalf of the gamete.

Your computer exports a Hotmoka node now, running on Tendermint. You can verify this with:

```shell
$ moka nodes manifest show
   takamakaCode: d840e1fb62c2655e7212c28d8b60ea22b2aab38f1c8eadba64118f61db96130e
   manifest: 01d03cf28428c89a9b3360477d141ba5716d11630272a52c534675f39f9e3553#0
      genesisTime: 2025-06-11T07:36:04.421951997Z
      chainId: chain-d5Y3D3
      ...
      allowsUnsignedFaucet: false
      gamete: 686601110429b794e762d7013bad9012289cab0204c357c916db4c8f654aca57#0
         balance: 1000000000000000000000000000000000000000000
         maxFaucet: 0
      validators: 01d03cf28428c89a9b3360477d141ba5716d11630272a52c534675f39f9e3553#1
         percent of validators' reward that gets staked: 75000000 (ie. 75.000000%)
         number of validators: 1
         validator #0: e8d06a563da12615a39baecf6bb041a9c39d795be094cc5803971f66cf54ecaf#0
           id: 0BF7FE524C2394DC905EF822B9DC654FB14ADB2E
           balance: 0
           staked: 0
           power: 1
      initialSupply: 1000000000000000000000000000000000000000000
      currentSupply: 1000000000000000000000000000000000000000000
      finalSupply: 2000000000000000000000000000000000000000000
      initialInflation: 100000 (ie. 0.100000%)
      currentInflation: 100000 (ie. 0.100000%)
      height: 1
      ...
```
If your computer is reachable at some address `my.machine` and if its 8001 port is open to the outside world,
then anybody can contact
your node at `ws://my.machine:8001`, query your node and run transactions on your node.
However, what has been created is a Tendermint node where all initial coins are inside
the gamete. By using the gamete, _you_ can fill the node with objects
and accounts now, and in general run all transactions you want.
However, other users, who do not know the keys of the gamete,
will not be able to run any non-`@View` transaction on your node.
If you want to open a faucet, so that other users can gain droplets of coins,
you must add the `--open-unsigned-faucet` option to the `moka nodes tendermint init`
command above. If you do that, you can then go _into another shell_ (since the previous one is busy with the
execution of the node), in a directory holding the key pair file of the gamete,
and type:

```shell
$ moka nodes faucet 5000000 --password
Enter value for --password (the password of the gamete account): mypassword
The threshold of the faucet has been set.
```

which specifies the maximal amount of coins that
the faucet is willing to give away at each request (its _flow_). You can re-run the `moka nodes faucet`
command many times, in order to change the flow of the faucet, or close it completely.
Needless to say, only the owner of the keys of the gamete can run the `moka nodes faucet` command,
which is why the key pair file of the gamete must be in the directory where you run `moka nodes faucet`.

After opening a faucet with a sufficient flow, anybody can
re-run the examples of the previous chapters by replacing
`ws://panarea.hotmoka.io:8001` and `ws://panarea.hotmoka.io:8002` with `ws://my.machine:8001`: your computer will serve
the requests and run the transactions.

If you turn off your Hotmoka node based on Tendermint, its state remains saved inside the
`chain` directory: the `chain/tendermint` subdirectory is where Tendermint stores the blocks
of the chain; while `chain/hotmoka` contains the Xodus database,
consisting of the storage objects created in blockchain.
If you stop the Tendermint node now (press enter in the window where it was running), you can subsequently
resume that node from its latest state, by typing:

```shell
$ moka nodes tendermint resume
The following service has been published:
 * ws://localhost:8001: the API of this Hotmoka node

Press the enter key to stop this process and close this node: 
```

There is a log file that can be useful to check the state of our Hotmoka-Tendermint app.
Namely, `tendermint.log` contains the log of Tendermint itself. It can be interesting
to inspect which blocks are committed and when:

```
I[2025-06-11|10:13:24.143] Version info, module=main
  tendermint_version=0.34.15 block=11 p2p=8
I[2025-06-11|10:13:24.169] Started node module=main
  nodeInfo="{ProtocolVersion:{P2P:8 Block:11 App:0}
I[2025-06-11|10:13:25.234] executed block module=state
  height=630 num_valid_txs=0 num_invalid_txs=0
I[2025-06-11|10:13:25.408] committed state module=state
  height=630 num_txs=0
  app_hash=A30F89457141AB7E94F71456871396FD9D30CA8E9F66998C6E3E3079D40849F
...
```
In ths log, the block height increases and the application hash changes whenever a block
contains transactions (`num_valid_txs`>0), reflecting the fact that the state has been modified.

## Disk Nodes

The Tendermint Hotmoka nodes of the previous section form a real blockchain.
They are perfect for deploying a blockchain where we can program smart contracts in
Takamaka. Nevertheless, they are slow for debugging: transactions are committed every few seconds,
by default. Hence, if we want to see the result of a transaction,
we have to wait for some seconds at least.
Moreover, Tendermint does not allow one to see the effects of each single transaction,
in a simple way. For testing, debugging and didactical purposes, it would be simpler to have a light node
that behaves like a blockchain, allows access to blocks and transactions as text files,
but is not a blockchain. This is the goal of the `DiskNode`s.
They are not part of an actual blockchain since they do not duplicate transactions
in a peer-to-peer network, where
consensus is imposed. But they are very
handy because they allow one to inspect, very easily, the requests sent to
the node and the corresponding responses.

You can start a disk Hotmoka node, with an open faucet, exactly as you did,
in the previous section for a Tendermint node, but using the `moka nodes disk init`
command instead of `moka nodes tendermint init`. You do not need any Tendermint configuration
this time, but still need a key to control the gamete of the node, that you can create
exactly as for a Tendermint Hotmoka node.
You then specify the Base58-encoded public key when starting the node:

```shell
$ moka nodes disk init
  ~/.m2/repository/io/hotmoka/io-takamaka-code/1.5.0/io-takamaka-code-1.5.0.jar
  --public-key-of-gamete=677QPRLfS4Mwgy2xA4dSEWmM3Hyb43mdZedSzq2g8Yxt
  --open-unsigned-faucet
Do you really want to start a new node at "chain" (old blocks and store will be lost) [Y/N] Y
The following service has been published:
 * ws://localhost:8001: the API of this Hotmoka node

The owner of the key pair of the gamete can bind it now to its address with:
  moka keys bind file_containing_the_key_pair_of_the_gamete --password
    --url url_of_this_Hotmoka_node
or with:
  moka keys bind file_containing_the_key_pair_of_the_gamete --password
    --reference 686601110429b794e762d7013bad9012289cab0204c357c916db4c8f654aca57#0

Press the enter key to stop this process and close this node: 
```

Then, in another shell, move in the directory holding the keys of the gamete, bind the
gamete to the keys and open the faucet:

```shell
$ moka keys bind gamete.pem --password
Enter value for --password (the password of the key pair): mypassword
The key pair of 686601110429b794e762d7013bad9012289cab0204c357c916db4c8f654aca57#0
  has been saved as "686601110429b794e762d7013bad9012289cab0204c357c916db4c8f654aca57#0.pem".
$ moka nodes faucet 5000000000000000 --password
Enter value for --password (the password of the gamete account): mypassword
The threshold of the faucet has been set.
```

You won't notice any real difference with Tendermint, but for the fact that this node is faster,
its default chain identifier is the empty string and it has no validators. Blocks and transactions are
inside the `chain` directory, that this time contains a nice textual representation of requests and
responses:

```shell
$ tree chain
chain
  hotmoka
    store
      b1
        0-d840e1fb62c2655e7212c28d8b60ea22b2aab38f1c8eadba64118f61db96130e
          request.txt
          response.txt
      b2
        0-686601110429b794e762d7013bad9012289cab0204c357c916db4c8f654aca57
          request.txt
          response.txt
      b3
        0-76c18f989bb9fbecab800fc86d1d76c9cca025aff3f32b61b4bc5b7d5cfb98f0
          request.txt
          response.txt
      b4
        0-9a65cdc535b724afadde2068f6dc6554d055bda6be6885ad87921793535cb1d1
          request.txt
          response.txt
      b5
        0-4bb66115dcfe21454203387af947da33f85057cc755f1d5ef86ea77b07fd6782
          request.txt
          response.txt
      b6
        0-e173fd80b965a282773ef99ded3e2ee3f73270e24f891bde6d7ea465625a3171
          request.txt
          response.txt
        1-ed2aa9b642579f31b70ed666c5d751a03f8ceed670a8f5e6f3608e0774970585
          request.txt
          response.txt
      b7
        0-5f3ca482f9ed97e86bf1609632dfa651c2892f316785bebf7a5c43f4bc6711f6
          request.txt
          response.txt
        1-f9c964b489333f34673517f76538b65da4dc29a2d0d2e46fdc337a5af4e0907e
          request.txt
          response.txt
```

> The exact ids and the number of these transactions will be different in your computer.

There are blocks, `b0`...`b7`, each containing a variable number of transactions.
Each transaction is reported with its id and the pair request/response that the node has computed
for it. They are text files, that you can open to understand what is happening inside the node.

The transactions shown above are those that have initialized the node and
opened the faucet. The last transaction inside each block is a _reward_
transaction, that distributes the earnings of the block to the (zero, for disk nodes) validators
and increases block height and number of transactions in the manifest.

Spend some time looking at the `request.txt` and `response.txt` files.
In particular, the last transaction inside `b7` should be that triggered by your `moka nodes faucet`
command. Open its `request.txt` file. It should read like this:

```
$ cat chain/hotmoka/store/b7/
    0-5f3ca482f9ed97e86bf1609632dfa651c2892f316785bebf7a5c43f4bc6711f6/request.txt
InstanceMethodCallTransactionRequestImpl:
  caller: 686601110429b794e762d7013bad9012289cab0204c357c916db4c8f654aca57#0
  nonce: 3
  gas limit: 100000
  gas price: 75
  class path: d840e1fb62c2655e7212c28d8b60ea22b2aab38f1c8eadba64118f61db96130e
  method: void io.takamaka.code.lang.Gamete.setMaxFaucet(java.math.BigInteger)
  actuals:
    5000000000000000
  receiver: 686601110429b794e762d7013bad9012289cab0204c357c916db4c8f654aca57#0
  chainId: 
  signature: 934391636ad2cafee0e6a8fc915a7e3ef4a85eb5b662ffe2f041c69161ae4...
```

You can clearly see that the `moka nodes faucet` command is called
the `setMaxFaucet` method of the gamete,
passing `5000000000000000` as new value for the flow of the faucet.
The caller (payer) and the receiver of the method invocation coincide, since they are both the
gamete. The signature has been generated with the keys of the gamete.

If you check the corresponding `response.txt`, you will see something like this:

```
$ cat chain/hotmoka/store/b7/
    0-5f3ca482f9ed97e86bf1609632dfa651c2892f316785bebf7a5c43f4bc6711f6/response.txt 
VoidMethodCallTransactionSuccessfulResponse:
  gas consumed for CPU execution: 1648
  gas consumed for RAM allocation: 3189
  gas consumed for storage consumption: 307
  updates:
    <686601110429b794e762d7013bad9012289cab0204c357c916db4c8f654aca57#0|
      io.takamaka.code.lang.Contract.balance:java.math.BigInteger|
      999999999999999999999999999999999999614200>
    <686601110429b794e762d7013bad9012289cab0204c357c916db4c8f654aca57#0|
      io.takamaka.code.lang.ExternallyOwnedAccount.nonce:java.math.BigInteger|
      4>
    <686601110429b794e762d7013bad9012289cab0204c357c916db4c8f654aca57#0|
      io.takamaka.code.lang.Gamete.maxFaucet:java.math.BigInteger|
      5000000000000000>
  events:
```

The response states clearly the cost of the transaction. Moreover, responses
typically report a set of _updates_. These are the side-effects on the state of the node,
induced by the transaction. Each update is a triple, that specifies a change in the value
of a field of a storage object. In this case, the three updates state that the
balance of the gamete has been decreased (because it paid for the transaction);
that its nonce has been increased to four (since it ran the transaction); and that the
`maxFaucet` field of the gamete has been set to 5000000000000000.

## Logs

All Hotmoka nodes generate a `hotmoka.log` log file, that reports which transactions have been
processed and potential errors (that file is stored in successive versions, so look for instance for
`hotmoka.log.0`, also in your home directory). Its content, in the case of a Tendermint node, looks like:

```
[2025-06-11 10:25:22] [INFO] 76c18f989bb9fbecab800fc86d1d76c9c...: delivering start
[2025-06-11 10:25:22] [INFO] 76c18f989bb9fbecab800fc86d1d76c9c...: delivering success
[2025-06-11 10:25:22] [INFO] e57b47e565cf4704d44ba2b04781eee84...: running start
[2025-06-11 10:25:22] [INFO] e57b47e565cf4704d44ba2b04781eee84...: running success
[2025-06-11 10:25:22] [INFO] 9a65cdc535b724afadde2068f6dc6554d...: posting
[2025-06-11 10:25:22] [INFO] 9a65cdc535b724afadde2068f6dc6554d...: delivering start
[2025-06-11 10:25:22] [INFO] 9a65cdc535b724afadde2068f6dc6554d...: delivering success
[2025-06-11 10:25:22] [INFO] e57b47e565cf4704d44ba2b04781eee84...: running start
[2025-06-11 10:25:22] [INFO] e57b47e565cf4704d44ba2b04781eee84...: running success
[2025-06-11 10:25:22] [INFO] e173fd80b965a282773ef99ded3e2ee3f...:
  4bb66115dcfe21454203387af947...#0 set as manifest
[2025-06-11 10:25:22] [INFO] e173fd80b965a282773ef99ded3e2ee3f...:
  the node has been initialized
[2025-06-11 10:25:22] [INFO] the gas station cache has been updated
  since it might have changed
[2025-06-11 10:25:22] [INFO] the gas cache has been updated since
  it might have changed: the new gas price is 100
[2025-06-11 10:25:22] [INFO] coinbase: units of gas consumed for CPU, RAM
  or storage since the previous reward: 0
[2025-06-11 10:25:22] [INFO] the gas cache has been updated since
  it might have changed: the new gas price is 75
[2025-06-11 10:25:22] [INFO] Started listener bound to [0.0.0.0:8001]
[2025-06-11 10:25:22] [INFO] node service(ws://localhost:8001): published
[2025-06-11 10:26:53] [INFO] node service(ws://localhost:8001):
  bound a new remote through session e0d52c8f-1f45-4bed-98e8-bf22cf7c027f
[2025-06-11 10:26:53] [INFO] node service(ws://localhost:8001):
  received a /get_manifest request
[2025-06-11 10:26:53] [INFO] node service(ws://localhost:8001):
  received a /get_takamaka_code request
[2025-06-11 10:26:54] [INFO] node service(ws://localhost:8001):
  unbound the remote at session e0d52c8f-1f45-4bed-98e8-bf22cf7c027f
```

If you want to follow in real time what is happening inside your node,
you can run for instance:

```shell
$ tail -f hotmoka.log
```

This will hang and print the new log entries as they are generated.
Assuming that you have a local node running in your machine, try for instance in another shell

```shell
$ moka nodes manifest show
```

You will see in the log all new entries related to the execution of the methods to access
the information on the node printed by the last command.

> Hotmoka nodes started with Docker disable the generation of the log files and dump
> logs to the standard output, where they can be accessed with the `docker logs` command.
> Therefore, they do not generate a `hotmoka.log` file. See next chapter for information.

## Node Decorators

__[See `io-hotmoka-tutorial-examples-runs` in `https://github.com/Hotmoka/hotmoka`]__

There are some frequent actions that can be performed on a Hotmoka node.
Typically, these actions consist in a sequence of transactions.
A few examples are:

1. The creation of an externally owned account. This requires the creation
   of its private and public keys and the instantiation of an
   `io.takamaka.code.lang.ExternallyOwnedAccount`. It is not a difficult
   procedure, but it is definitely tedious and occurs frequently.
2. The installation of a jar in a node. This requires a transaction for installing
   code in the node. It requires also to parse the jar into bytes and identify the
   number of gas units for the transaction, depending on the size of the jar.
3. The initialization of a node. Namely, local nodes start empty, that is,
   their store does not contain anything at the beginning, not even their manifest
   object. This initialization is rather technical and detail might change in future
   versions of Hotmoka. Performing this initialization by hand leads to fragile
   and error-prone code.

In all these examples, Hotmoka provides decorators, that is, implementations of the
`Node` interface built from an existing `Node` object. A decorator is just an alias
of the decorated node, but adds some functionality or performs some action on it.
Figure 34 shows that there are decorators for each of the three
situations enumerated above.

In order to understand the use of node decorators and appreciate their existence,
let us write a Java class that creates a `DiskNode`, hence initially empty;
then it initializes that node; subsequently it installs our `io-hotmoka-tutorial-examples-family-1.9.0.jar`
file in the node and finally creates two accounts in the node. We stress the fact that
these actions
can be performed in code by using calls to the node interface (Figure 34);
but they can also be performed through the `moka` tool. Here, however, we want to perform them
in code, simplified by using node decorators.

Create the following `Decorators.java` class inside the `io.hotmoka.tutorial.examples.runs` package of the
`io-hotmoka-tutorial-examples-runs` project:

```java
package io.hotmoka.tutorial.examples.runs;

import static io.hotmoka.constants.Constants.HOTMOKA_VERSION;
import static io.takamaka.code.constants.Constants.TAKAMAKA_VERSION;

import java.math.BigInteger;
import java.nio.file.Paths;
import java.security.KeyPair;

import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.helpers.AccountsNodes;
import io.hotmoka.helpers.InitializedNodes;
import io.hotmoka.helpers.JarsNodes;
import io.hotmoka.node.ConsensusConfigBuilders;
import io.hotmoka.node.disk.DiskNodeConfigBuilders;
import io.hotmoka.node.disk.DiskNodes;

public class Decorators {
 
  public static void main(String[] args) throws Exception {
    var config = DiskNodeConfigBuilders.defaults().build();

    // the path of the runtime Takamaka jar, inside Maven's cache
    var takamakaCodePath = Paths.get
      (System.getProperty("user.home") +
      "/.m2/repository/io/hotmoka/io-takamaka-code/" + TAKAMAKA_VERSION
      + "/io-takamaka-code-" + TAKAMAKA_VERSION + ".jar");

    // the path of the user jar to install
    var familyPath = Paths.get(System.getProperty("user.home")
      + "/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family/"
      + HOTMOKA_VERSION
      + "/io-hotmoka-tutorial-examples-family-" + HOTMOKA_VERSION + ".jar");

    // create a key pair for the gamete
    var signature = SignatureAlgorithms.ed25519();
    var entropy = Entropies.random();
    KeyPair keys = entropy.keys("mypassword", signature);
    var consensus = ConsensusConfigBuilders.defaults()
   	  .setInitialSupply(BigInteger.valueOf(1_000_000_000))
   	  .setPublicKeyOfGamete(keys.getPublic()).build();

	 try (var node = DiskNodes.init(config)) {
      // first view: store the io-takamaka-code jar and create manifest and gamete
      var initialized = InitializedNodes.of(node, consensus, takamakaCodePath);

      // second view: store the family jar: the gamete will pay for that
      var nodeWithJars = JarsNodes.of(node, initialized.gamete(), keys.getPrivate(), familyPath);

      // third view: create two accounts, the first with 10,000,000 units of coin
      // and the second with 20,000,000 units of coin; the gamete will pay
      var nodeWithAccounts = AccountsNodes.of
        (node, initialized.gamete(), keys.getPrivate(),
        BigInteger.valueOf(10_000_000), BigInteger.valueOf(20_000_000));

      System.out.println("manifest: " + node.getManifest());
      System.out.println("family jar: " + nodeWithJars.jar(0));
      System.out.println("account #0: " + nodeWithAccounts.account(0) +
                         "\n  with private key " + nodeWithAccounts.privateKey(0));
      System.out.println("account #1: " + nodeWithAccounts.account(1) +
                         "\n  with private key " + nodeWithAccounts.privateKey(1));
    }
  }
}
```

Run class `Decorators`:
```shell
$ cd io-hotmoka-tutorial-examples-runs
$ mvn clean install exec:exec -Dexec.executable="java"
    -Dexec.args="-cp %classpath io.hotmoka.tutorial.examples.runs.Decorators"
```
It should print something like this on the console:

```
manifest: 1883cbb309a2f0d70e41b2e8587808c8f2cb45eb5c881e3a3603b78add16f568#0
family jar: f401c4599e5b94393b6fb3799cd36d7de9c4162e83c4487feab416e921c036bd
account #0: 88a94f374b677f507cd7f48a1f05dc51cf20b871dc6ab23eee076d21be9da31b#2
  with private key Ed25519 Private Key [60:bb:8a:57:1d:53...aa:23:22:12]
    public data: 3da4498e4523ef0d53f778141801b2c2c4fb184f0c5fa41b8936db940d94903b

account #1: 88a94f374b677f507cd7f48a1f05dc51cf20b871dc6ab23eee076d21be9da31b#5
  with private key Ed25519 Private Key [44:ad:6c:2c:b5:c2...59:d7:45:04]
    public data: 84e329ac4df2e74be504167625733953df1620b082b7e5805029f583eda7050c
```

You can see that the use of decorators has avoided us the burden of
programming transaction requests, explicitly, and makes our code more robust,
since future versions of Hotmoka will update the implementation of the decorators,
while their interface will remain untouched, shielding our code from modifications.

As we have already said, decorators are
views of the same node, just seen through different lenses
(Java interfaces). Hence, further transactions can be run on
`node` or `initialized` or `nodeWithJars` or `nodeWithAccounts`, with the same
effects. Moreover, it is not necessary to close all such nodes: closing `node` at
the end of the try-with-resource will actually close all of them, since they are the same node.

## Hotmoka Services

__[See `io-hotmoka-tutorial-examples-runs` in `https://github.com/Hotmoka/hotmoka`]__

This section shows how we can publish a Hotmoka node online, by using Java code,
so that it becomes a
network service that can be used, concurrently, by many remote clients.
Namely, we will show how to publish a blockchain node based on Tendermint, but the code
is similar if you want to publish a memory Hotmoka node or any
other Hotmoka node.

Remember that we have already published our nodes online, by using the
`moka nodes tendermint init` and `moka nodes disk init` commands.
Here, however, we want to do the same operation in code.

Create a class `Publisher.java` inside package `io.hotmoka.tutorial.examples.runs`
of the `io-hotmoka-tutorial-examples-runs` project,
whose code is the following:

```java
package io.hotmoka.tutorial.examples.runs;

import java.math.BigInteger;
import java.nio.file.Paths;
import java.security.KeyPair;

import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.helpers.InitializedNodes;
import io.hotmoka.node.ValidatorsConsensusConfigBuilders;
import io.hotmoka.node.service.NodeServices;
import io.hotmoka.node.tendermint.TendermintNodeConfigBuilders;
import io.hotmoka.node.tendermint.TendermintNodes;
import io.takamaka.code.constants.Constants;

public class Publisher {
  public static void main(String[] args) throws Exception {
    var config = TendermintNodeConfigBuilders.defaults().build();

    // the path of the runtime Takamaka jar, inside Maven's cache
    var takamakaCodePath = Paths.get
      (System.getProperty("user.home") +
      "/.m2/repository/io/hotmoka/io-takamaka-code/" + Constants.TAKAMAKA_VERSION +
      "/io-takamaka-code-" + Constants.TAKAMAKA_VERSION + ".jar");

    // create a key pair for the gamete
    var signature = SignatureAlgorithms.ed25519();
    var entropy = Entropies.random();
    KeyPair keys = entropy.keys("password", signature);
    var consensus = ValidatorsConsensusConfigBuilders.defaults()
      .setPublicKeyOfGamete(keys.getPublic())
      .setInitialSupply(BigInteger.valueOf(100_000_000))
      .build();

    try (var original = TendermintNodes.init(config);
      // remove the next line if you want to publish an uninitialized node
      // var initialized = InitializedNodes.of(original, consensus, takamakaCodePath);
      var service = NodeServices.of(original, 8001)) {

      System.out.println("\nPress ENTER to turn off the server and exit this program");
      System.in.read();
    }
  }
}
```

We have already seen that `original` is a Hotmoka node based on Tendermint.
The subsequent line makes the feat:

```java
var service = NodeServices.of(original, 8001);
```

Variable `service` holds a Hotmoka _service_, that is, an actual network service that adapts
the `original` node to a web API that is published at localhost, at port 8001.
The service is an `AutoCloseable` object: it starts when it is created and gets shut down 
when its `close()` method is invoked, which occurs, implicitly, at the end of the
scope of the try-with-resources. Hence, this service remains online until the user
presses the ENTER key and terminates the service (and the program).

Run class `Publisher`:

```shell
$ cd io-hotmoka-tutorial-examples-runs
$ mvn clean install exec:exec -Dexec.executable="java"
    -Dexec.args="-cp %classpath io.hotmoka.tutorial.examples.runs.Publisher"
```
It should work for a few seconds and then start waiting for the ENTER key. Do not press such key yet!
Since `original` is not initialized yet, it has no manifest and no gamete. Its store is just empty
at the moment. You can verify that by running:

```shell
$ moka nodes manifest show
The remote service is misbehaving: are you sure that it is actually published
  at ws://localhost:8001 and that it is initialized and accessible?
```

which fails since it cannot find a manifest in the node.

Thus, let us initialize the node before publishing it, so that it is already
initialized when published. Press ENTER to terminate the service, then modify
the `Publisher.java` class by uncommenting the use of the `InitializedNode` decorator,
whose goal is to create manifest and gamete of the node
and install the basic classes of the Takamaka runtime inside the node.

> Note that we have published `original`:
>
> ```java
> var service = NodeServices.of(original, 8001);
> ```
>
> We could have published `initialized` instead:
>
> ```java
> var service = NodeServices.of(initialized, 8001);
> ```
>
> The result would be the same, since both are views of the same node object.
> Moreover, note that we have initialized the node inside the try-with-resources,
> before publishing the service as the last of the three resources.
> This ensures that the node, when published, is already initialized.
> In principle, publishing an uninitialized node, as done previously, exposes
> to the risk that somebody else might initialize the node, hence taking its control
> since he will set the keys of the gamete.

If you re-run class `Publisher` and retry the `moka nodes manifest show` command, you should see
the manifest of the now initialized node on the screen.

> A Hotmoka node, once published, can be accessed by many
> users, _concurrently_. This is not a problem, since Hotmoka nodes are thread-safe and can
> be used in parallel by many users. Of course, this does not mean that there are no
> race conditions at the application level. As a simple example, if two users operate
> with the same paying externally owned account, their wallets might suffer from race
> conditions on the nonce of the account and they might see requests
> rejected because of an incorrect nonce. The situation is the same here as in Ethereum,
> for instance. In practice, each externally owned account should be controlled by a single wallet
> at a time.

## Remote Nodes

We have seen how a service can be published and its methods called through
a browser. This is easy for methods such as `getManifest()` and
`getConfig()` of the interface `Node`. However, it
becomes harder if we want to call methods of `Node` that need parameters, such
as `getState()` or the many `add/post/run` methods for scheduling transactions on
the node. Parameters should be passed as JSON payload of the websockets connection, in a format
that is hard to remember, easy to get wrong and possibly changing in the future.
Moreover, the JSON responses must be parsed back.
In principle, this can be done by hand or through software that builds the
requests for the server and interprets its responses.
Nevertheless, it is not the suggested way to proceed.

A typical solution to this problem is to provide a software SDK, that is, a library
that takes care of serializing the requests into JSON and deserializing
the responses from JSON. Roughly speaking, this is the approach taken in Hotmoka.
More precisely,
we can forget about the details of the JSON serialization
and deserialization of requests and responses and only program against the `Node` interface,
by using an adaptor of a published Hotmoka service into a `Node`. This adaptor is called
a _remote_ Hotmoka node.

We have used remote nodes from the very beginning of this tutorial.
Namely, if you go back to [Installation of the Jar in a Hotmoka Node](#installation-of-the-jar-in-a-hotmoka-node),
you will see that we have built a Hotmoka node from a remote service:

```java
try (var node = RemoteNodes.of(URI.create("ws://panarea.hotmoka.io:8001"), 80000)) {
  ...
}
```
The `RemoteNodes.of(...)` method adapts a remote service into a Hotmoka node,
so that we can call all methods of that (Figure 34). The
`80000` parameter is the timeout, in milliseconds, for connecting to the service
and for the methods called on the remote node.

### Creating Sentry Nodes

We have seen that a `Node` can be published as a Hotmoka service:
on a machine `my.validator.com` we can execute:

```java
TendermintNodeConfig config = TendermintNodeConfigBuilders.defaults().build();

try (Node original = TendermintNodes.init(config);
     NodeService service = NodeServices.of(original, 8001)) {
  ...
}
```

The service will be published on the internet at `ws://my.validator.com:8001`.
Moreover, on another machine `my.sentry.com`,
that Hotmoka service can be adapted into a remote node
that, itself, can be published on that machine:

```java
try (Node validator = RemoteNodes.of(URI.create("ws://my.validator:8001"), 80000);
     NodeService service = NodeServices.of(validator, 8001)) {
  ...
}
```

The service will be published at `ws://my.sentry.com:8001`.

We can continue this process as much as we want, but let us stop at this point.
Programmers can connect to the service published at
`ws://my.sentry.com:8001` and send requests to it. That service is just a bridge
that forwards everything to the service at `ws://my.validator.com:8001`.
It might not be immediately clear why this intermediate step could be useful
or desirable. The motivation is that we could keep the (precious) validator
machine under a firewall that allows connections with `my.sentry.com` only.
As a consequence, in case of DOS attacks, the sentry node will receive
the attack and possibly crash, while the validator continues to operate as usual:
it will continue to interact with the other validators and take part in the validation
of blocks. Moreover, since many sentries can be connected to a single validator, the latter
remains accessible through the other sentries, if needed.
This is an effective way to mitigate the problem of DOS attacks to validator nodes.

The idea of using sentry nodes against DOS attacks is not new for proof-of-stake networks,
whose validators are considered as precious resources that must be protected. It is used, for
instance, in Cosmos networks [[Sentry]](#references).
However, note how it is easy, with Hotmoka,
to build such a network architecture by using network
services and remote nodes.

## Signatures and Quantum-Resistance

Hotmoka is agnostic wrt. the algorithm used for signing requests. This means that it is
possible to deploy Hotmoka nodes that sign requests with distinct signature algorithms.
Of course, if nodes must re-execute the same transactions, such as in the case of a
blockchain, then all nodes of the blockchain must use the same algorithm for
the transactions signed by each given account, or otherwise
they will not be able to reach consensus.
Yet, any algorithm can be chosen for the blockchain. In principle, it is even possible to use
an algorithm that does not sign the transactions, if the identity of the callers of the
transactions needn't be verified. However, this might be sensible in private networks only.

The default signature algorithm used by a node is specified at construction time, as a configuration
parameter. For instance, the code

```java
var config = TendermintNodeConfigBuilders.defaults().build();
var consensus = ValidatorsConsensusConfigBuilders.defaults()
  .setPublicKeyOfGamete(keys.getPublic())
  .setInitialSupply(SUPPLY)
  ....
  .setSignatureForRequests(SignatureAlgorithms.ed25519()) // this is the default
  .build();

try (Node node = TendermintNodes.init(config);
     Node initialized = InitializedNodes.of(node, consensus, takamakaCodePath)) {
  ...
}
```

starts a Tendermint-based blockchain node that uses the ed25519 signature algorithm
as default signature algorithm for the requests.
Requests sent to that node can be signed as follows:

```java
// recover the algorithm used by the node
SignatureAlgorithm signature = node.getConfig().getSignatureForRequests();

// create a key pair for that algorithm
KeyPair keys = signature.getKeyPair();

// create a signer object with the private key of the key pair
Signer<SignedTransactionRequest<?>> signer = signature.getSigner
  (keys.getPrivate(), SignedTransactionRequest<?>::toByteArrayWithoutSignature);

// create an account having public key keys.getPublic()
....

// create a transaction request on behalf of the account
ConstructorCallTransactionRequest request
  = TransactionRequests.constructorCall(signer, account, ...);

// send the request to the node
node.addConstructorCallTransaction(request);
```

In the example above, we have explicitly specified
to use ed25519 as default signature algorithm. That is what is chosen
if nothing is specified at configuration-time.
Consequently, there is no need to specify that algorithm in the
configuration object and that is why we never did it in the previous chapters.
It is possible to configure nodes with other default signature algorithms.
For instance:

```java
var consensus = ValidatorsConsensusConfigBuilders.defaults()
  .setPublicKeyOfGamete(keys.getPublic())
  .setInitialSupply(SUPPLY)
  ....
  .setSignatureForRequests(SignatureAlgorithms.sha256dsa()) // this replaces the default
  .build();
```

configures a node that uses sha256dsa as default signature algorithm, while

```java
var consensus = ValidatorsConsensusConfigBuilders.defaults()
  .setPublicKeyOfGamete(keys.getPublic())
  .setInitialSupply(SUPPLY)
  ....
  .setSignatureForRequests(SignatureAlgorithms.empty())
  .build();
```

configures a node that uses the empty signature as default signature algorithm; it is an
algorithm that accepts all signatures, in practice disabling signature checking.

It is possible to specify a quantum-resistant signature algorithm as default,
that is, one that belongs to
a family of algorithms that are expected to be immune from attacks performed through
a quantistic computer. For instance,

```java
var consensus = ValidatorsConsensusConfigBuilders.defaults()
  .setPublicKeyOfGamete(keys.getPublic())
  .setInitialSupply(SUPPLY)
  ....
  .setSignatureForRequests(SignatureAlgorithms.qtesla1())
  .build();
```

configures a node that uses the quantum-resistant qtesla-p-I algorithm as default signature algorithm,
while

```java
var consensus = ValidatorsConsensusConfigBuilders.defaults()
  .setPublicKeyOfGamete(keys.getPublic())
  .setInitialSupply(SUPPLY)
  ....
  .setSignatureForRequests(SignatureAlgorithms.qtesla3())
  .build();
```

configures a node that uses the quantum-resistant qtesla-p-III
algorithm as default signature algorithm, that is expected to be more resistant than
qtesla-p-I but has larger signatures than qtesla-p-I.

Quantum-resistance is an important aspect of future-generation blockchains.
However, at the time of this writing, a quantum attack is mainly a theoretical
possibility, while the large size of quantum-resistant keys and signatures is
already a reality and a node using a qtesla signature algorithm _as default_
might exhaust the disk space of your computer very quickly. In practice, it is better
to use a quantum-resistant signature algorithm only for a subset of the transactions, whose
quantum-resistance is deemed important. Instead, one should use a lighter algorithm
(such as the default ed25519) for all other transactions. This is possible because
Hotmoka nodes allow one to mix transactions signed with distinct algorithms.
Namely, one can use ed25519 as default algorithm, for all transactions signed
by instances of `ExternallyOwnedAccount`s,
with the exception of those transactions that are signed by instances of
`AccountQTESLA1`, such as `ExternallyOwnedAccountQTESLA1`,
or of `AccountQTESLA3`, such as `ExternallOwnedAccountQTESLA3`,
or of `AccountSHA256DSA`, such as `ExternallOwnedAccountSHA256DSA`
(see Figure 23).
Namely, if the caller of a transaction is an `AccountQTESLA1`, then the
request of the transaction must be signed with the qtesla-p-I algorithm.
If the caller of a transaction is an `AccountQTESLA3`, then the
request of the transaction must be signed with the qtesla-p-III algorithm.
If the caller of a transaction is an `AccountSHA256DSA`, then the
request of the transaction must be signed with the sha256dsa algorithm.
If the caller of a transaction is an `AccountED25519`, then the
request of the transaction must be signed with the ed25519 algorithm.
In practice, this allows specific transactions to override the default signature
algorithm for the node.

For instance, let us create an account that uses the default signature algorithm for the node.
We charge its creation to the faucet of the node:

```shell
$ moka keys create --name account7.pem --password
Enter value for --password (the password that will be needed later to use the key pair): game
The new key pair has been written into "account7.pem":
...
$ moka accounts create faucet 1000000000000 account7.pem --password
    --uri ws://panarea.hotmoka.io:8001
Enter value for --password (the password of the key pair specified through --keys): game
Adding transaction 01dba6799acb73e90a152cc236886334204038d0f9be4666a9886d9dbce3d05a... done.
A new account @account7 has been created.
Its key pair has been saved into the file
  "@account7.pem".

Gas consumption:
 * total: 6757
   * for CPU: 2461
   * for RAM: 3824
   * for storage: 472
   * for penalty: 0
 * price per unit: 1 pana
 * total price: 6757 panas
```

You can check the class of the new account with the `moka objects show` command:

```shell
$ moka objects show @account7
    --uri ws://panarea.hotmoka.io:8001
class io.takamaka.code.lang.ExternallyOwnedAccountED25519
    (from jar installed at 48a393014e839351b1bf56bdf9f127601f49b938d27e2c890fc8dd3e08099182)
  io.takamaka.code.lang.Contract.balance:java.math.BigInteger = 1000000000000
  io.takamaka.code.lang.ExternallyOwnedAccount.nonce:java.math.BigInteger = 0
  io.takamaka.code.lang.ExternallyOwnedAccount.publicKey:java.lang.String
    = jeoCtIYV6NalOR9QTnUDBQubIN1h+UmqU91Yvl0Wtxc=
```

As you can see, an account has been created, that uses the default `ed25519`
signature algorithm of the node.
Assume that we want to create an account now, that _always_ uses the `sha256dsa` signature algorithm,
regardless of the default signature algorithm of the node. We can specify that to `moka accounts create`:

```shell
$ moka keys create --name account8.pem --password --signature sha256dsa
Enter value for --password (the password that will be needed later to use the key pair): play
The new key pair has been written into "account8.pem":
...
$ moka accounts create faucet 1000000000000 account8.pem --password
    --uri ws://panarea.hotmoka.io:8001
    --signature sha256dsa
Enter value for --password (the password of the key pair specified through --keys): play
Adding transaction 8c875a9e12e85e3304a1c29c9a556b25dbb34f5821b6ebc5013a02f6f35d2e02... done.
A new account 8c875a9e12e85e3304a1c29c9a556b25dbb34f5821b6ebc5013a02f6f35d2e02#0 has been created.
Its key pair has been saved into the file
  "8c875a9e12e85e3304a1c29c9a556b25dbb34f5821b6ebc5013a02f6f35d2e02#0.pem".

Gas consumption:
 * total: 8912
   * for CPU: 2461
   * for RAM: 3824
   * for storage: 2627
   * for penalty: 0
 * price per unit: 1 pana
 * total price: 8912 panas
```
This creation has been more expensive than for the previous account, because the public key of the
sha256dsa algorithm is much longer than that for the ed25519 algorithm.
You can verify this with the `moka objects show` command:

```shell
$ moka objects show 8c875a9e12e85e3304a1c29c9a556b25dbb34f5821b6ebc5013a02f6f35d2e02#0
    --uri ws://panarea.hotmoka.io:8001
class io.takamaka.code.lang.ExternallyOwnedAccountSHA256DSA
    (from jar installed at 48a393014e839351b1bf56bdf9f127601f49b938d27e2c890fc8dd3e08099182)
  io.takamaka.code.lang.Contract.balance:java.math.BigInteger = 1000000000000
  io.takamaka.code.lang.ExternallyOwnedAccount.nonce:java.math.BigInteger = 0
  io.takamaka.code.lang.ExternallyOwnedAccount.publicKey:java.lang.String
    = MIIDRzCCAjkGByqGSM44BAEwggIsAoIBAQCVR1z12T5ZbD/NHZAq3QL0J/XzxyEDE...
...
```

Note that the class of the account is `ExternallyOwnedAccountSHA256DSA` this time.

Let us create an account that uses the qtesla-p-I signature algorithm now:

```shell
$ moka keys create --name account9.pem --password --signature qtesla1
Enter value for --password (the password that will be needed later to use the key pair): quantum1
The new key pair has been written into "account9.pem":
...
$ moka accounts create faucet 1000000000000 account9.pem --password
    --uri ws://panarea.hotmoka.io:8001
    --signature qtesla1
Enter value for --password (the password of the key pair specified through --keys): quantum1
Adding transaction beed61bc4733dae8907045278b07c1d522b11a85147ef71d71edfe8d2301cbb7... done.
A new account beed61bc4733dae8907045278b07c1d522b11a85147ef71d71edfe8d2301cbb7#0 has been created.
Its key pair has been saved into the file
  "beed61bc4733dae8907045278b07c1d522b11a85147ef71d71edfe8d2301cbb7#0.pem".

Gas consumption:
 * total: 46415
   * for CPU: 2461
   * for RAM: 3824
   * for storage: 40130
   * for penalty: 0
 * price per unit: 1 pana
 * total price: 46415 panas
```
The creation of this account has been still more expensive, since this kind of quantum-resistant
keys are very large. Again, you can use the `moka object show`
command to verify that it has class `ExternallyOwnedAccountQTESLA1`.

Finally, let us use the previous qtesla-p-I account to create a qtesla-p-III account:

```shell
$ moka keys create --name account10.pem --password --signature qtesla3
Enter value for --password (the password that will be needed later to use the key pair): quantum3
The new key pair has been written into "account10.pem":
...
$ moka accounts create beed61bc4733dae8907045278b07c1d522b11a85147ef71d71edfe8d2301cbb7#0
    100000 account10.pem
    --password --uri ws://panarea.hotmoka.io:8001
    --signature=qtesla3 --password-of-payer
Enter value for --password (the password of the key pair): quantum3 
Enter value for --password-of-payer (the password of the payer): quantum1
Do you really want to create the new account spending up to 6300000 gas units
  at the price of 1 pana per unit (that is, up to 6300000 panas) [Y/N] Y
Adding transaction cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b2160... done.
A new account cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#00 has been created.
Its key pair has been saved into the file
  "cfc1fcb3ab32f177e8f07538d79bcf8db4ed19cc7d650857e83d855db445b216#00.pem".

Gas consumption:
 * total: 111003
   * for CPU: 2012
   * for RAM: 3536
   * for storage: 105455
   * for penalty: 0
 * price per unit: 1 pana
 * total price: 111003 panas
```

Note, again, the extremely high gas cost of this creation.

Regardless of the kind of account, their use is always the same.
The only difference is to use the right signature algorithm when signing
a transaction, since it must match that of the caller account. This is automatic, if we
use the `moka` tool. For instance, let us use our qtesla-p-I account to install
the `io-hotmoka-tutorial-examples-family-1.9.0.jar` code in the node:

```shell
$ cd hotmoka_tutorial
$ moka jars install beed61bc4733dae8907045278b07c1d522b11a85147ef71d71edfe8d2301cbb7#0
    io-hotmoka-tutorial-examples-family/target/io-hotmoka-tutorial-examples-family-1.9.0.jar
    --password-of-payer --uri=ws://panarea.hotmoka.io:8001
Enter value for --password-of-payer (the password of the key pair of the payer account): quantum1
Do you really want to install the jar spending up to 1011600 gas units
  at the price of 1 pana per unit (that is, up to 1011600 panas) [Y/N] Y
Adding transaction 0dcb77728ec268825d645b0b5114c1a1290d93db3c53bb849918bf5f5da0221f... done.
The jar has been installed at 0dcb77728ec268825d645b0b5114c1a1290d93db3c53bb849918bf5f5da0221f.

Gas consumption:
 * total: 11858
   * for CPU: 1618
   * for RAM: 3308
   * for storage: 6932
   * for penalty: 0
 * price per unit: 1 pana
 * total price: 11858 panas
```

The `moka` tool has understood that the payer is an account that signs with the
qtesla-p-I algorithm and has signed the request accordingly.

# Tendermint Hotmoka Nodes

Section [Tendermint Nodes](#tendermint-nodes)
has already presented the implementation of Hotmoka nodes based on Tendermint.
These nodes have the architecture shown in Figure 35 and
form an actual blockchain network. Since the underlying blockchain engine is
Tendermint, the resulting network is based on a proof of stake consensus. That is,
there is a selected subset of the nodes (the _validators_) that decide if transactions
are legal and which must be included in blockchain. Other nodes can just verify the
transactions.

The methodology for starting Tendermint nodes,
described in Section [Tendermint Nodes](#tendermint-nodes), is relatively complex. Moreover, it is
fragile, since the node might have a slightly different behavior, depending on the
exact configuration of the machine and of the exact version of the Java runtime installed
in the machine. These differences might even compromise the capacity of a network
of nodes to reach consensus. There is a simpler and less fragile
way of installing a Tendermint Hotmoka node, by using the docker tool.
We cannot discuss docker here. We just say that it is a utility to run lightweight
containers in a machine. A container is a sort of preconfigured
sandbox, whose configuration is fixed
and already provided at installation time. Therefore, there is nothing to install if we use
a Docker container to start the node, not even a Java runtime. Everything is already
prepared inside the container. Well, our machine must have docker installed, but that is the
only requirement.

## Starting a Tendermint Hotmoka Node with Docker

There are preconfigured Docker images of Hotmoka nodes, in Docker Hub, that you can see
at [https://hub.docker.com/u/hotmoka](https://hub.docker.com/u/hotmoka).
For instance, it is possible to run the Tendermint Hotmoka node image to see its available commands:

```shell
$ docker run -it hotmoka/tendermint-node:1.9.0 info
This container manages Hotmoka nodes using Tendermint as byzantine consensus engine.

This container could be run with two volumes,
one for the chain directory and another for the configuration
directory of the node, and mapping all ports that could be used
by Hotmoka or Tendermint:
  docker run -it -p 8001:8001 -p 26656:26656 -v chain:/home/hotmoka/chain 
    -v hotmoka_tendermint:/home/hotmoka/hotmoka_tendermint
    hotmoka/tendermint-node:VERSION /bin/bash

The following commands and options are available inside the container:

* info:         print this information message

* config-init:  create the configuration directory of the first node
   of a brand new blockchain
    ALLOWS_UNSIGNED_FAUCET: true if the unsigned faucet must be opened
    CHAIN_ID: the chain identifier of the blockchain
    INITIAL_SUPPLY: the initial supply of coins of the blockchain
    FINAL_SUPPLY: the final supply of coins of the blockchain
    PUBLIC_KEY_OF_GAMETE: the Base64-encoded public key of the gamete of the blockchain
    TARGET_BLOCK_CREATION_TIME: the milliseconds between two successive blocks
    TOTAL_VALIDATION_POWER: the total units of validation power,
      initially owned by the first validator account

* config-start: create the configuration directory of a new node
      of an existing blockchain
    HOTMOKA_PUBLIC_SERVICE_URI: the URI of an already existing node of the blockchain
    TARGET_BLOCK_CREATION_TIME: the milliseconds between two successive blocks

* init:         create a node for a brand new blockchain, whose configuration
    has been created with config-init

* start:        create a node that connects to an already existing node of a blockchain,
    whose configuration has been created with config-start

* resume:       resume a node whose container was previously turned off
```

If you didn't do it in the previous chapter, you should now create the key pair of the gamete:

```shell
$ moka keys create --name gamete.pem --password
Enter value for --password (the password that will be needed later to use the key pair): mypassword
The new key pair has been written into "gamete.pem":
* public key: 677QPRLfS4Mwgy2xA4dSEWmM3Hyb43mdZedSzq2g8Yxt (ed25519, base58)
* public key: S9so3T9QIAau/zTpsAlKEhkkJOsqV3HDIQCc72ITha0= (ed25519, base64)
* Tendermint-like address: 8380F3BEC1568BC3A07DE0CA721BA91CD9BC5282
```

It is now possible to create the configuration directory of a Tendermint node of a new blockchain,
by using the `config-init` command of the same Docker image:

```shell
$ docker run -it
    -e TARGET_BLOCK_CREATION_TIME="4000"
    -e PUBLIC_KEY_OF_GAMETE="S9so3T9QIAau/zTpsAlKEhkkJOsqV3HDIQCc72ITha0="
    -e CHAIN_ID="jellyfish"
    -e ALLOWS_UNSIGNED_FAUCET="true"
    -p 8001:8001 -p 26656:26656
    -v chain:/home/hotmoka/chain
    -v hotmoka_tendermint:/home/hotmoka/hotmoka_tendermint
    hotmoka/tendermint-node:1.9.0
    config-init

I will use the following parameters for the creation of the configuration directory
of a Hotmoka node using Tendermint as byzantine consensus engine:

      ALLOWS_UNSIGNED_FAUCET=true
                    CHAIN_ID="jellyfish"
              INITIAL_SUPPLY="1000000000000000000000000000000000"
                FINAL_SUPPLY="10000000000000000000000000000000000"
        PUBLIC_KEY_OF_GAMETE="S9so3T9QIAau/zTpsAlKEhkkJOsqV3HDIQCc72ITha0="
  TARGET_BLOCK_CREATION_TIME=4000
      TOTAL_VALIDATION_POWER=1000000

Cleaning the configuration directory...done
Creating the validator.pem key pair of the node as validator... done
Creating the local Hotmoka node configuration file... done
Creating the consensus Hotmoka node configuration file... done
Creating the Tendermint configuration files... done
```

The configuration is created inside the `/home/hotmoka/hotmoka_tendermint` directory of the docker container.
Since that directory is mapped to the volume `hotmoka_tendermint` of the host machine, it will remain
available also after the docker image terminates the execuiton of `config-init`. In this way, when we next
run the `init` command, the docker container will find the configuration created by `config-init` inside
the `hotmoka_tendermint` and use it to start the new node:

```shell
$ docker run -it
    -p 8001:8001 -p 26656:26656
    -v chain:/home/hotmoka/chain
    -v hotmoka_tendermint:/home/hotmoka/hotmoka_tendermint
    hotmoka/tendermint-node:1.9.0
    init
```

You should see the log of the node, that creates and initializes a new Hotmoka node based on Tendermint:

```
Starting a Hotmoka node based on Tendermint as the single initial node
  of a brand new blockchain.
[2025-06-12 06:58:24] [INFO] opened the store database at chain/hotmoka/store
[2025-06-12 06:58:25] [INFO] Tendermint ABCI started at port 26658
[2025-06-12 06:58:25] [INFO] the Tendermint process is up and running
[2025-06-12 06:58:25] [INFO] Tendermint started at port 26657
...
The following service has been published:
 * ws://localhost:8001: the API of this Hotmoka node

The validators are the following accounts:
 * b437832a688dbb89b145d380b7cb3eb841d7cb09fb600d27be43cd670e8b43f9#0
     with public key 5kqX4XcJv6q4EymCbT9RTHSCCALdKSmyqScnxVtJKvfL (ed25519, base58)

The owner of the key pair of the gamete can bind it now to its address with:
  moka keys bind file_containing_the_key_pair_of_the_gamete --password
    --url url_of_this_Hotmoka_node
or with:
  moka keys bind file_containing_the_key_pair_of_the_gamete --password
    --reference fcc6ad9a4cd4109dcb554df6f1d951f770d42cdb4c2caeb8fa713c1d4189b4fa#0

Press the enter key to stop this process and close this node:
[2025-06-12 06:58:54] [INFO] garbage-collected store 7d73a2309c9885299c2a...
[2025-06-12 06:58:57] [INFO] coinbase: behaving validators:
  030203B36BA4EDF0182D7D40D7EA7FE34A9415B4...
[2025-06-12 06:58:57] [INFO] coinbase: units of gas consumed
  for CPU, RAM or storage since the previous reward: 0
...
```
If you press the enter key, the node will be turned off and the docker container will exit.
You can start the node again using the `-d` docker option, that runs it in the background:

```shell
$ docker run -dit
    -p 8001:8001 -p 26656:26656
    -v chain:/home/hotmoka/chain
    -v hotmoka_tendermint:/home/hotmoka/hotmoka_tendermint
    hotmoka/tendermint-node:1.9.0
    init

360afca6f5bcf864b0f8430c4c8b8e1058101f13ea6921e9d911a5f09fa375a9
$
```

Wait for around 30 seconds, in order to give time to the node to start and initialize. After that time, the node should be up
and running in your local machine, as you can verify with `moka nodes manifest show`:

```shell
$ moka nodes manifest show --uri ws://localhost:8001
    takamakaCode: 3d7ac2ef69a3f989a9cbaa17825a4f8fa577c8b604c72d2d5b5d4a3fd5caf7cc
    manifest: 1ee56c42f51c27ac64f1513774f15be6009c6b22459b9651e885addba782c3f3#0
      genesisTime: 2025-06-12T06:57:12.441531326Z
      chainId: jellyfish
      maxErrorLength: 300
      maxDependencies: 20
      maxCumulativeSizeOfDependencies: 10000000
      allowsUnsignedFaucet: true
      skipsVerification: false
      signature: ed25519
      gamete: fcc6ad9a4cd4109dcb554df6f1d951f770d42cdb4c2caeb8fa713c1d4189b4fa#0
        balance: 1000000000000000000000000000000000
        maxFaucet: 0
    ...
    validators: 1ee56c42f51c27ac64f1513774f15be6009c6b22459b9651e885addba782c3f3#1
       ...
       number of validators: 1
       validator #0: b437832a688dbb89b145d380b7cb3eb841d7cb09fb600d27be43cd670e8b43f9#0
         id: 030203B36BA4EDF0182D7D40D7EA7FE34A9415B4
          balance: 0
          staked: 0
          power: 1000000
```

> Since `--uri ws://localhost:8001` is the default, you can just type
> `moka nodes manifest show`. The same holds for all other `moka` commands.

In order to use the gamete, we must bind the key to its storage reference:

```shell
$ moka keys bind gamete.pem --password
Enter value for --password (the password of the key pair): 
The key pair of fcc6ad9a4cd4109dcb554df6f1d951f770d42cdb4c2caeb8fa713c1d4189b4fa#0
  has been saved as "fcc6ad9a4cd4109dcb554df6f1d951f770d42cdb4c2caeb8fa713c1d4189b4fa#0.pem".
```

That's all. We can now use the gamete to open the faucet of the node (`moka nodes faucet`) and play
with the node as we did in the previous chapters of this book. Just direct the clients
to `ws://localhost:8001` instead of `ws://panarea.hotmoka.io:8001`.

Let us analyze the options passed to `docker`. The `run -dit` command means that we want to
instantiate, and run as an interactive daemon,
a docker image, which is actually specified at the end:
`hotmoka/tendermint-node:1.9.0`. Docker will download that image from Docker Hub.

> That image assumes that you are using a Linux machine based on the amd64 architecture.
> If you are using a Linux machine based on the arm64 architecture, use the
> `hotmoka/tendermint-node-arm64:1.9.0` image. If you are using a Windows machine,
> you need to run a Linux image inside a Linux virtual machine, as always in docker.
> Please refer to the docker documentation to know how this can be accomplished.

A specific command of the image is run, specified at the end: the first time it was
`config-init`, that creates a configuration directory, the second time it was
`init`, that initializes a Tendermint Hotmoka node as specified in the configuration directory.

> The configuration directory generated by `config-init` should be fine for most uses.
> If you want to fine tune it, you can run, between `config-init` and `init`,
> the same docker container with the `/bin/bash` command and edit the
> content of the `hotmoka_tendermint` directory. Inside the container, the `vi`
> editor is available.

Options are passed to the docker image as environment variables,
through the `-e` switch. The `TARGET_BLOCK_CREATION_TIME` value is the time, in milliseconds,
between the creation of two consecutive blocks; the `PUBLIC_KEY_OF_GAMETE` value is the Base64-encoded
public key to assign to the gamete of the node, that is, to the account that, initially, holds
all cryptocurrency in the blockchain; the `CHAIN_ID` value if the chain identifier to assign to
the new blockchain.
The `OPEN_UNSIGNED_FAUCET` switch opens a free faucet for getting cryptocurrency for free from the
gamete: use `false` for a real blockchain, of course.

As shown in Figure 35, a Tendermint Hotmoka node communicates
to the external world through ports 26656 for gossip and 8001 (or 8002, or 80 or any other port) for clients.
Hence those ports must be connected to the docker image. We do that with the
`-p` switch. Specifically, in our example, port 8001 of the real machine is mapped to port 8001 of the docker image
and port 26656 of the real machine is mapped to port 26656 of the docker image. For instancem, if you prefer to use
port 80 to expose the new node, you should write `-p 80:8001` instead of `-p 8001:8001`.

Finally, the blocks and state created by the node are saved into a `/home/hotmoka/chain`
directory inside the container that is mapped to a docker volume `chain`. That
volume is visible in the real machine as `/var/lib/docker/volumes/chain/_data/` and will be
persisted if we turn the docker container off.

> The actual directory that contains the volume depends on the operating system
> and on the specific version of docker.
> Currently, it is `/var/lib/docker/volumes/chain/_data/` in Linux machines and you must be
> root to access it.

The `docker run` command printed a hash at the end, that identifies the running container.
We can use it, for instance, to turn the container off when we do not need it anymore:

```shell
$ docker stop 360afca6f5bcf864b0f8430c4c8b8e1058101f13ea6921e9d911a5f09fa375a9
```

> The hash will be different in your experiments. Use yours.

You can verify that the Tendermint Hotmoka node is not available anymore:

```shell
$ moka nodes manifest show
The remote service is misbehaving: are you sure that it is actually published
  at ws://localhost:8001 and that it is initialized and accessible?
```

However, the data of the blockchain still exists, inside its directory of the
host machine:
`/var/lib/docker/volumes/chain/_data/`. Hence, it is possible to resume the execution of the
blockchain from its final state. Restrain from using again
the `init` command
for that: it would create a brand new blockchain again, from scratch, destroying the local data
of the previous blockchain.
Unless this is actually what you want to achieve,
the right command to resume a previously started and stopped blockchain from its saved state is

```shell
$ docker run -dit
    -p 8001:8001 -p 26656:26656
    -v chain:/home/hotmoka/chain
    -v hotmoka_tendermint:/home/hotmoka/hotmoka_tendermint
    hotmoka/tendermint-node:1.9.0
    resume

c1407e499ad67465318704da1fcb6e9b88ee94faceb7c4b86e00ab4775590b3f
```

Wait for a few seconds and then verify that the _same_ node is back:

```shell
$ moka nodes manifest show
    takamakaCode: 3d7ac2ef69a3f989a9cbaa17825a4f8fa577c8b604c72d2d5b5d4a3fd5caf7cc
    manifest: 1ee56c42f51c27ac64f1513774f15be6009c6b22459b9651e885addba782c3f3#0
      genesisTime: 2025-06-12T06:57:12.441531326Z
      chainId: jellyfish
      maxErrorLength: 300
      maxDependencies: 20
      maxCumulativeSizeOfDependencies: 10000000
      allowsUnsignedFaucet: true
      skipsVerification: false
      signature: ed25519
      gamete: fcc6ad9a4cd4109dcb554df6f1d951f770d42cdb4c2caeb8fa713c1d4189b4fa#0
        balance: 1000000000000000000000000000000000
        maxFaucet: 0
    ...
```

Turn the node off now and conclude our experiment:

```shell
$ docker stop c1407e499ad67465318704da1fcb6e9b88ee94faceb7c4b86e00ab4775590b3f
```

We have discussed the `info`, `config-init`, `init` and `resume` commands of the docker image.
Section [Connecting a Tendermint Hotmoka Node to an Existing Blockchain](#connecting-a-tendermint-hotmoka-node-to-an-existing-blockchain)
will show an example of use of the `config-start` and `start` commands.
Before that, let us understand better what the manifest of a Tendermint node tells us.

## Manifest and Validators

The information reported by `moka nodes manifest show` referes to two accounts that have been
created during the initialization of the node:

```shell
gamete: fcc6ad9a4cd4109dcb554df6f1d951f770d42cdb4c2caeb8fa713c1d4189b4fa#0
  balance: 1000000000000000000000000000000000
  maxFaucet: 0

validator #0: #0
  id: 030203B36BA4EDF0182D7D40D7EA7FE34A9415B4
  balance: 0
  staked: 0
  power: 1000000
```

We already know the first one, that is, the gamete. Its private key is not stored in the docker container
but must be available to the person who started the container.
Normally, it is the key that was created before starting the node (with `moka keys create`) and
that is later bound to the storage address of the gamete (with `moka keys bind`). If you
followed the instructions in the previous section, you should have an
`.pem` file in your file system, for the gamete. With that pem file, you have _superuser_ rights,
in the sense that you can, for instance,
open and close the faucet (but only if you started the node with the `ALLOWS_UNSIGNED_FAUCET` option set to true).
Moreover, you own all cryptocurrency initially minted for the node! With that, you can create and fund as many new accounts
as you want and in general run any transaction you like.

There is a second account that has been created. Namely, the _validator_ account, at address
`b437832a688dbb89b145d380b7cb3eb841d7cb09fb600d27be43cd670e8b43f9` (this will be different in your node).
This is an externally owned account that gets remunerated for every non-`@View`
transaction run in the node and included in blockchain. The previous print-out shows that, at
the beginning, the balance of the validator is 0. Let us resume the node, run a transaction (for instance,
create a new account by letting the faucet pay) and check what happens.

```shell
$ docker run -dit 
    -p 8001:8001 -p 26656:26656
    -v chain:/home/hotmoka/chain
    -v hotmoka_tendermint:/home/hotmoka/hotmoka_tendermint
    hotmoka/tendermint-node:1.9.0
    resume
c1407e499ad67465318704da1fcb6e9b88ee94faceb7c4b86e00ab4775590b3f

$ moka keys create --name account.pem --password
Enter value for --password (the password that will be needed later to use the key pair): squid
The new key pair has been written into "account.pem":
* public key: 58ibUYbJ1unUNYFTk6DkMQe8qGDd2p9AU1qnJ1vurKfn (ed25519, base58)
* public key: PWjYJCCrskFCgr0Y2QP6pPTUTa2sH4m3QvRwG5kOn9k= (ed25519, base64)
* Tendermint-like address: 00CCBC3965B3E623B06E07230C489AA300B215F1

$ moka accounts create fcc6ad9a4cd4109dcb554df6f1d951f770d42cdb4c2caeb8fa713c1d4189b4fa#0 1234567 account.pem --password --password-of-payer
Enter value for --password (the password of the key pair): squid
Enter value for --password-of-payer (the password of the payer): mypassword 
Do you really want to create the new account spending up to 200000 gas units
  at the price of 1 pana per unit (that is, up to 200000 panas) [Y/N] Y
Adding transaction 960e4b9ccd642b1d903feed73003b910260fd808de0ff206fb34085d085ea726... done.
A new account 960e4b9ccd642b1d903feed73003b910260fd808de0ff206fb34085d085ea726#0 has been created.
Its key pair has been saved into the file
  "960e4b9ccd642b1d903feed73003b910260fd808de0ff206fb34085d085ea726#0.pem".

Gas consumption:
 * total: 6266
   * for CPU: 2165
   * for RAM: 3616
   * for storage: 485
   * for penalty: 0
 * price per unit: 1 pana
 * total price: 6266 panas
```

The gas consumed for this transaction has been forwarded to the validators of the blockchain, at its current price.
Since there is only a single validator, everything goes to it, as you can verify:

```shell
$ moka nodes manifest show
...
gamete: fcc6ad9a4cd4109dcb554df6f1d951f770d42cdb4c2caeb8fa713c1d4189b4fa#0
  balance: 999999999999999999999999998759167
  maxFaucet: 0

validator #0: b437832a688dbb89b145d380b7cb3eb841d7cb09fb600d27be43cd670e8b43f9#0
  id: 030203B36BA4EDF0182D7D40D7EA7FE34A9415B4
  balance: 1568
  staked: 4704
  power: 1000000
```
The information above tells us that the gamete has now a reduced balance:
it paid 1000000000000000000000000000000000 - 999999999999999999999999998759167
that is 1240833 panareas to create the new account.
Of these, 1234567 went to the balance of the new account. The remaining
1240833 - 1234567, that is 6266, have been paid for gas. The gas price
was at one panarea per gas unit at the time of creating the account.
This means that 6266 coins have been paid to the only validator of the blockchain,
whose balance actually increased to 1568 coins plus 4704 _staked_ coins, for a total of
6272 coins. It is important to note that the 6266 panareas did not go
immediately to the only validator: as shown above, only 1568 have been paid immediately;
other 4704 have been staked for that validator, that is, kept in the validators contract
as a motivation for the validator to behave correctly. In the future, if the validator misbehaves
(that is, does not validate the transactions correctly or does not validate them at all) then
this stake will be reduced by a percent that is called _slashing_.
This is by default 1% for validators that do not validate correctly and 0.5% for
validators that do not validate at all (for instance, they are down).
The staked amount of panareas will be forwarded to the validator only when it will sell all its
validation power to another validator and stop being a validator.

There is a final remark. We said that 6266 panareas have been forwarded to the validator
(immediately or staked). But it actually received 6272 panareas.
Where do these 6 extra panareas
come from? They have been _minted_, that is, created from scratch as a form of _inflation_.
By default, the initial inflation is 0.1%. It is actually the case that 6 is the 0.1%
of 6266 (approximatively).

We have understood that the validator account receives payments for the validation of transactions.
But who controls this validator? It turns out that the `config-init` command
created the key pair of this validator inside the configuration directory of the node.
You can see it if you access the `hotmoka_tendermint` volume where the container operates.
You must be root to do that:

```shell
$ sudo ls /var/lib/docker/volumes/hotmoka_tendermint/_data
consensus_config.toml  local_config.toml  tendermint_config  validator.pem
```
In alternative, you can use the `docker exec` command to run a command inside the container.
You do not need to be root, but need to know the id of the running container (`docker ps` might help you):

```shell
$ docker exec c1407e499ad67465318704da1fcb6e9b88ee94faceb7c4b86e00ab4775590b3f
    /bin/ls hotmoka_tendermint

consensus_config.toml
local_config.toml
tendermint_config
validator.pem
```

Who owns that key controls the validator. Therefore, you might wanto to move it in your host machine:

```shell
$ docker cp c1407e499:/home/hotmoka/hotmoka_tendermint/validator.pem .
Successfully copied 2.05kB
```

As a final remark about the key of the validator, note that it _must_ be the same
key that the underlying Tendermint engine uses in order to identify the node in the network
and vote for validation. If that is not the case, the validator account in the
manifest will not be recognized as a working validator and will be slashed for
not behaving. Eventually, it will be expulsed from the set of validators.
Tendermint stores the key that it uses to identify the node in another file, inside
its configuration, and in JSON format:

```shell
$ docker exec c1407e499 /bin/ls hotmoka_tendermint/tendermint_config/config
config.toml
genesis.json
node_key.json
priv_validator_key.json
```
This file must remain in the node, or otherwise Tendermint cannot vote for validation.
The docker script magically ensures that, correctly, this file contains the same key
as `validator.pem`, although in a different format.

## Starting a Tendermint Hotmoka Node on Amazon EC2

In the previous section,
we have published a Hotmoka node on our machine (the localhost).
This might not be the best place where
the node should be published, since our machine might not allow external internet connections
and since we might want to turn that machine off after we stop working with it.
In reality, a node should be published inside a machine that can receive external connections and
that is always on, at least for a long period. There are many solutions for that.
Here, we describe the simple technique of using a rented machine from Amazon AWS
EC2's computing cloud [[EC2]](#references).
This service offers a micro machine for free, while more powerful machines
require one to pay for their use. Since the micro machine is enough for our purposes,
EC2 is a good candidate for experimentation. Nevertheless, what we describe in this section
can be achieved with any other cloud rental architecture.

Conceptually, there is nothing special with running a Tendermint Hotmoka node
inside an Amazon EC2 machine: start a free microservice machine, ssh to it and
execute the commands of the previous section, but inside the EC2 machine.
You will probably need to install 
Docker in the EC2 machine, by following the standard installation instructions for Docker
in a Linux machine, that you can find at
[https://docs.docker.com/engine/install/](https://docs.docker.com/engine/install/).
As Figure 35 shows, ports 80 (for instance) and 26656 must be open
in order for a Tendermint Hotmoka node to connect to the outside world and work correctly.
Therefore, use a browser to access
the Amazon EC2 console, select your micromachine and inspect its security group. Modify its
inbound rules so that they allow connections to ports 22 (for ssh), 26656 (for gossip)
and 80 (or 8001 or 8002 or whichever port you like, for clients).
At the end, such rules should look as in Figure 36.

 <p align="center"><img width="800" src="pics/inbound_rules.png" alt="Figure 36. The inbound rules for the Amazon EC2 machine"></p><p align="center">Figure 36. The inbound rules for the Amazon EC2 machine.</p>


For the rest, the instructions of the previous chapter will let you start a Hotmoka Tendermint node on the EC2 machine.
We suggest to create the `gamete.pem` file in your local machine and only use its public key to configure
the node in the EC2 machine, so that the key of the gamete never enters the remote EC2 machine.

You can inspect the logs of you container as usual with docker:

```shell
ec2$ docker logs 5f3799b58c6569b50dbebee6db3061ebf8e3c2c7ac6e0882579129ca66302786
```

(assuming 5f3799b58c6569b50dbebee6db3061ebf8e3c2c7ac6e0882579129ca66302786 is the id of your container)
which will print all logs of the container. If you want to see the logs in real time,
while they are generated by the container, you can run

```shell
ec2$ docker logs -f 5f3799b58c6569b50dbebee6db3061ebf8e3c2c7ac6e0882579129ca66302786
```

Remember that Docker places the logs in a JSON file accessible in the container directory of the
EC2 machine (you must be root to see that directory):

```shell
ec2$ sudo ls /var/lib/docker/containers
              /5f3799b58c6569b50dbebee6db3061ebf8e3c2c7ac6e0882579129ca66302786

5f3799b58c6569b50dbebee6db3061ebf8e3c2c7ac6e0882579129ca66302786-json.log
...
```

If you start the docker container with the `-d` option, you can sefely exit the remote machine and the node
will continue working in the background. When you do not need the node anymore, you can turn off its container
with `docker stop`.

## Connecting a Tendermint Hotmoka Node to an Existing Blockchain

We have started, and subsequently turned off, a blockchain consisting of a single validator
node, living in splendid isolation. But we might want to do something different.
Namely, we want now to start a node that _belongs_ to the same blockchain network as
`ws://panarea.hotmoka.io:8002`. This means that the node will be started and will replay
all transactions already executed by `ws://panarea.hotmoka.io:8002`, starting from the empty state.
At the end, our node will be a clone of `ws://panarea.hotmoka.io:8002`, with its same state.
From that moment on, our node will execute the same transactions that
reach the blockchain of `ws://panarea.hotmoka.io:8002`, and also send new transactions to that
blockchain, if they reach our node. It will be a _peer_ of that blockchain network.
Note, however, that it will not be a validator of the blockchain.

The process here is the same as for the creation of a brand new blockchain of a single
node. The only difference is that we create the configuration of the node with the
`config-start` command, instead of `config-init`, and that we start the node
with the `start` command, instead of `init`.

Let us create the configuration of the node then. Start by stopping any docker container that you might have
running on your local machine. Since this is a peer of an existing blockchain,
whose gamete already exists, we do not need any gamete key pair to start with. We create
its configuration in a way that mirrors that of the node at `ws://panarea.hotmoka.io:8002`:

```shell
$ docker run -it
    -e TARGET_BLOCK_CREATION_TIME="4000"
    -e HOTMOKA_PUBLIC_SERVICE_URI="ws://panarea.hotmoka.io:8002"
    -p 8001:8001 -p 26656:26656
    -v chain:/home/hotmoka/chain
    -v hotmoka_tendermint:/home/hotmoka/hotmoka_tendermint
    hotmoka/tendermint-node:1.9.0
    config-start

Going to create the configuration directory of a Hotmoka node using
  the Tendermint byzantine consensus engine, with the following parameters:

 HOTMOKA_PUBLIC_SERVICE_URI=1.9.0
 TARGET_BLOCK_CREATION_TIME=4000

Cleaning the directory hotmoka_tendermint... done
Creating the validator.pem key pair of the node as validator... done
Creating the local Hotmoka node configuration file... done
Extracting the Tendermint configuration files to match those at ws://panarea.hotmoka.io:8002... 
  MANIFEST=...
  CHAIN_ID=...
  GENESIS_TIME=...
  ...
done

$ docker run -dit
    -p 8001:8001 -p 26656:26656
    -v chain:/home/hotmoka/chain
    -v hotmoka_tendermint:/home/hotmoka/hotmoka_tendermint
    hotmoka/tendermint-node:1.9.0
    start

$
```

Wait a few seconds, to give the node the time to start synchronization.
After that, you can verify that the node in your local machine is a clone
of that at `ws://panarea.hotmoka.io:8002`. Just compare the output of
`moka nodes manifest show` and `moka nodes manifest show --uri ws://panarea.hotmoka.io:8002`.

> It is possible to start a peer of another node manually, with no use of docker, similarly to what
> we have done in section [Tendermint Nodes](#tendermint-nodes).
> We highly discourage the attempt,
> since it requires one to create a Tendermint configuration that mirrors
> that of the remote cloned node, which is not trivial. Moreover, the peer must run exactly the
> same Java runtime as the cloned node, or otherwise the two machines
> might not reach consensus about the effects of the transactions.
> By using a prepared docker image, we save us such headache.
> The interested reader can see the implementation of that image
> inside the distribution of Hotmoka, in the `io-hotmoka-docker` project.

In general, the `start` command might take a while, since it downloads and replays all transactions
already executed in the cloned node. Therefore, if you turn the started node off with the `docker stop` command,
as done before, and then want it on again, it is not convenient to start the clone again, since
it will download and replay the transactions again, from scratch. Instead, use the `resume` command with
`docker run`, as we have already done before. This makes the node resume from its old state, so that
it will only download and replay the transactions that have been done in the remote node since the stop of the clone.

## Shared Entities

This section describes how the set of validators
is implemented in Hotmoka. Namely, the validation power of the network is expressed as a
total quantity shared among all validator nodes. For instance, when we have shown the manifest of the
nodes, we have seen information about the only validator in the subsequent form:

```shell
$ moka nodes manifest show
...
validator #0: b437832a688dbb89b145d380b7cb3eb841d7cb09fb600d27be43cd670e8b43f9#0
  id: 030203B36BA4EDF0182D7D40D7EA7FE34A9415B4
  balance: 1568
  staked: 4704
  power: 1000000
```

This means that validator #0 has a _power_ of 1000000. Since it is the only validator of the network,
the total power of the validators of the network is 1000000.
This validator can decide to sell part of its power or all its power to
another validator, resulting in a network with a single (different) validator or with more validators.
For instance, it might sell 200000 units of power to another validator #1, resulting
in a network with two validators: validator #0 with 800000 units of power and
validator #1 with 200000 units of power.

The power of a validator expresses the weight of its votes: in order to validate a transaction,
at least two thirds of the validation power must agree on the outcome of the transaction, or
otherwise the network will hang.
This mechanism is inherited from the underlying Tendermint engine and might be different
in other Hotmoka nodes in the future. However, the idea that power represents the weight
of a validator will likely remain.

What said above means that the validators are a sort of _entity_ that shares validation
power among each single validator. Validation power can be sold and bought. The number of
the validators and their power is consequently dynamic. In some sense, this mechanism
resembles the market of shares of a corporation.

 <p align="center"><img width="700" src="pics/entities.png" alt="Figure 37. The hierarchy of entities and validators classes"></p><p align="center">Figure 37. The hierarchy of entities and validators classes.</p>


Hotmoka has an interface that represents entities whose shares can be dynamically sold and bought about _shareholders_.
Figure 37 shows this `SharedEntity` interface. As you can see in the figure, the notion of validators
is just a special case of shared entity (see also [[BeniniGMS21]](#references)).
It is possible to use shared entities to represent other concepts, such as
a distributed autonomous organization, or a voting community. Here, however, we focus on their use to represent the set
of the validators of a proof of stake blockchain.

In general, two concepts are specific to each implementation of shared entities:
who are the potential shareholders and how offers for selling shares work.
Therefore, the interface `SharedEntity<S,O>` has two type
variables: `S` is the type of the shareholders and O is the type of the sale offers of shares.
The `SharedEntityView` interface at the top of the hierarchy in Figure 37 defines
the read-only operations on a shared entity. This view is static, in the sense
that it does not specify the operations for transfers of shares. Therefore, its
only type parameter is `S`: any contract can play the role of the type for the
shareholders of the entity. Method `getShares` yields the current
shares of the entity (who owns how much).
Method `isShareholder` checks if an object is a shareholder. Method
`sharesOf` yields the number of shares that a shareholder owns. As typical in Takamaka,
the `snapshot` method allows one to create a frozen read-only copy of an entity
(in constant time), useful when an entity must be queried from a client without
the risk of race conditions if another client is modifying the same entity concurrently.

The `SharedEntity` subinterface adds methods for transfer of shares.
It includes an inner class `Offer` that models sale offers: it specifies
who is the seller of the shares, how many shares are being sold, the requested
price and the expiration of the offer. Method `isOngoing` checks if an offer has
not expired yet. Implementations can subclass `Offer` if they need more specific
offers. Offers can be placed on sale by calling the `place` method with a sale
offer. This method is annotated as `@FromContract` since the caller must be
identified as the owner of the shares
(or otherwise anybody could sell the shares of anybody else) and as
`@Payable` so that implementations can require to pay a ticket to place shares on
sale. The sale offer is passed as a parameter to `place`, hence it must have been
created before calling that method. The set of all sale offers is available through
`getOffers`. Method `sharesOnSaleOf` yields the cumulative number of shares on
sale for a given shareholder. Who wants to buy shares calls method `accept` with
the accepted offer and with itself as `buyer`
and becomes a new shareholder or increases its cumulative number of shares
(if it was already a shareholder). Also this method is `@Payable`, since its caller
must pay `ticket >= offer.cost` coins to the seller. This means that shareholders
must be able to receive payments and that is why `S extends PayableContract`.
The `SimpleSharedEntity` class implements the shared entity algorithms, that subclasses
can redefine if they want.

Hotmoka models validator nodes as
objects of class `Validator`, that are externally owned accounts with an extra identifier
(Figure 37).
In the specific case of a Hotmoka blockchain built over Tendermint, validators
are instances of the subclass `TendermintED25519Validator`, whose identifier is derived from their
ed25519 public key. This identifier is public information, reported
in the blocks or easily eavesdropped.
The `Validators` interface in Figure 37
extends the `SharedEntity` interface, fixes the shareholders
to be instances of `Validator` and adds two methods: `getStake` yields the amount of coins
at stake for each given validator (if the validator misbehaves, its stake will
be slashed); and `reward`, that is called by the blockchain itself at
the end of each block creation: it distributes the cost of the gas consumed by
the transactions in the block, to the well-behaving validators, and slashes the
stakes of the misbehaving validators.

The `AbstractValidators` class implements the validators’ set and the distribution
of the reward and is a subclass of `SimpleSharedEntity` (see Figure 37).
Shares are voting power in this case. Its subclass `TendermintValidators`
restricts the type of the validators to be `TendermintED25519Validator`. At each
block committed, Hotmoka calls the reward method of `Validators` in order
to reward the validators that behaved correctly and slash those that
misbehaved, possibly removing them from the validators' set. They are specified by
two strings that contain the identifiers of the validators, as provided by the
underlying Tendermint engine. At block creation time, Hotmoka
calls method `getShares` and informs
the underlying Tendermint engine about the identifiers of the validator nodes
for the next blocks. Tendermint expects such validators to mine and vote the
subsequent blocks, until a change in the validators’ set occurs.

## Becoming a Validator

Up to now, we have started Tendermint Hotmoka nodes of a blockchain with only a single validator node.
That is, the network can have more nodes, but only one of them
(the one started first) is the validator of the
network. All other nodes are simply peers, that verify the transactions but have no voice
in the network and do not vote for validation.

The configuration of a HOtmoka Tendermint node, created thorugh the `config-init` or `config-start` commands,
contains a `validator.pem` key pair file, as said above. You can use that file to create an actual
account object, a candidate for becoming a validator of the network. You can do this with the
`moka nodes tendermint validators create` command, similar to
`moka accounts create` but that creates instances of `TendermintED25519Validator`.
Once you have a validator object for your `validator.pem`, you can become an actual validator
when one of the existing validators decides to sell (part of) its shares and you buy it.
The seller validator creates a sale offer with the `moka nodes tendermint validators sell`
command. The sale offer will be visible in the manifest of the node and can be bought with
the `moka nodes tendermint validators buy` command. After that, the buyer will be a new
validator of the network.

> It is important to note that a validator node is assumed to be reachable from the outside world
> and up, also overnight. Most home desktop computers, connected through a modem, have no public IP
> that can be used to reach them from the outside world (the public IP of the modem reaches the modem
> itself, not the computer). Therefore, if you try to use a home machine to become a validator, your
> machine will be immediately slashed for being uneachable and it will be removed from the set of validators,
> immediately after becoming one. You need a machine with a public IP for being a validator, such as
> for instance a machine on Amazon EC2 or on a similar rental service.

# Code Verification

Code verification checks that code complies with some constraints, that should
guarantee that its execution does not run into errors. Modern programming
languages apply more or less extensive code verification, since this helps
programmers write reliable code. This can both occur at run time and at compile
time. Run-time (_dynamic_) code verification is typically stronger, since it can exploit
exact information about the actual run-time values flowing through the code. However,
compile-time (_static_) code verification has the advantage that it runs only
once, at compilation time or at jar installation time, and can prove, once and for all,
that some errors will never occur, regardless of the execution path that will
be followed at run time.

Hotmoka nodes apply a combination of static and dynamic verification to the
Takamaka code that is installed inside their store.
Static verification runs only once, when a node installs
a jar in its store, or when classes are loaded for the first time at run time.
Dynamic verification runs every time some piece of code gets executed.

## JVM Bytecode Verification

Takamaka code is written in Java, compiled into Java bytecode, instrumented
and run inside the Java Virtual Machine (_JVM_). Hence, all code verifications
executed by the JVM apply to Takamaka code as well. In particular, the JVM verifies
some structural and dynamic constraints of class files, including their
type correctness.
Moreover, the JVM executes run-time checks as well: for instance, class casts
are checked at run time, as well as pointer dereferences and
array stores. Violations result in exceptions. For a thorough discussion,
we refer the interested
reader to the official documentation about Java bytecode class
verification [[JVM-Verification]](#references).

## Takamaka Bytecode Verification

Hotmoka nodes verify extra constraints, that are not checked as part of the
standard JVM bytecode verification. Such extra constraints are mainly related to
the correct use of Takamaka annotations and contracts, and are
in part static and in part dynamic. Static constraints are checked when a
jar is installed into the store of a node, hence only once for each node of a network.
If a static constraint
is violated, the transaction that tries to install the jar fails with
an exception. Dynamic constraints, instead,
are checked every time a piece of code is run. If a dynamic constraint is
violated, the transaction that runs the code fails with an exception.

Below, remember that `@FromContract` is shorthand for `@FromContract(Contract.class)`.
Moreover, note that the constraints related
to overridden methods follow by Liskov's principle [[LiskovW94]](#references).

Hotmoka nodes verify the following static constraints:

1. The `@FromContract(C.class)` annotation is only applied to constructors of a
  (non-strict) subclass of `io.takamaka.code.lang.Storage` or to instance methods of a
  (non-strict) subclass of `io.takamaka.code.lang.Storage` or of an interface.
2. In every use of the `@FromContract(C.class)` annotation, class `C` is a subclass
  of the abstract class `io.takamaka.code.lang.Contract`.
3. If a method is annotated as `@FromContract(C.class)` and overrides another method,
  then the latter is annotated as `@FromContract(D.class)` as well, and `D` is a
  (non-strict) subclass of `C`.
4. If a method is annotated as `@FromContract(D.class)` and is overridden by another method,
  then the latter is annotated as `@FromContract(C.class)` as well, and `D` is a
  (non-strict) subclass of `C`.
5. If a method is annotated as `@Payable`, then it is also annotated as
  `@FromContract(C.class)` for some `C`.
6. If a method is annotated as `@Payable`, then it has a first formal argument
  (the paid amount) of type `int`, `long` or `BigInteger`.
7. If a method is annotated as `@Payable` and overrides another method,
  then the latter is annotated as `@Payable` as well.
8. If a method is annotated as `@Payable` and is overridden by another method,
  then the latter is annotated as `@Payable` as well.
9. The `@Payable` annotation is only applied to constructors of a (non-strict) subclass of
  `io.takamaka.code.lang.Contract` or to instance methods of a (non-strict) subclass of
  `io.takamaka.code.lang.Contract` or of an interface.
10. Classes that extend `io.takamaka.code.lang.Storage` have instance non-transient
  fields whose type
  is primitive (`char`, `byte`, `short`, `int`, `long`, `float`,
  `double` or `boolean`), or is a class that extends `io.takamaka.code.lang.Storage`,
  or is any of
  `java.math.BigInteger`, `java.lang.String`, `java.lang.Object` or is an interface
  (see [Storage Types and Constraints on Storage Classes](#storage-types-and-constraints-on-storage-classes)).

> The choice of allowing, inside a storage type, fields of type
> `java.lang.Object` can be surprising. After all, any reference value can be
> stored in such a field, which requires to verify, at run time, if the field
> actually contains a storage value or not (see the dynamic check number 5, below).
> The reason for this choice is to allow generic storage types, such as
> `StorageTreeMap<K,V>`, whose values are storage values as long as `K` and `V`
> are replaced with storage types. Since Java implements generics by erasure,
> the bytecode of such a class ends up having fields of type `java.lang.Object`. An alternative
> solution would be to bound `K` and `V` from above
> (`StorageTreeMap<K extends Storage, V extends Storage>`). This second choice
> will be erased by using `Storage` as static type of the erased fields of the
> class. However, not all storage reference values extend `Storage`. For instance,
> this solution would not allow one to write `StorageTreeMap<String, BigInteger>`, where
> both `String` and `BigInteger` are storage types, but neither extends `Storage`.
> The fact that fields of type `java.lang.Object` or interface actually hold a
> storage value at the end of a transaction is checked dynamically (see the
> dynamic checks below).

11. There are no static initializer methods.

> Java runs static initializer methods the first time their defining class is loaded. They
> are either coded explicitly, inside a `static { ... }` block, or are
> implicitly generated by the compiler in order to initialize the static fields
> of the class. The reason for forbidding such static initializers is that,
> inside Takamaka, they would end up being run many times, at each transaction
> that uses the class, and reset the static state of a class,
> since static fields are not kept in blockchain.
> This is a significant divergence from the expected
> semantics of Java, that requires static initialization of a class to
> occur only once during the lifetime of that class. Note that the absence of
> static initializers still allows a class to have static fields, as long as
> they are bound to constant primitive or `String` values.

12. There are no finalizers.

> A finalizer is a method declared exactly as
> `public void finalize() { ... }`. It might be called
> when the JVM garbage collects an object from RAM.
> The reason for forbidding such finalizers is that
> their execution is not guaranteed (they might never be called)
> or might occur at a non-deterministic moment,
> while code in blockchain must be deterministic.

13. Calls to `caller()` occur only inside `@FromContract` constructors or methods
    and on `this`.
14. Calls to constructors or methods annotated as `@FromContract` occur
    only in constructors or instance methods of an
    `io.takamaka.code.lang.Contract`; moreover, if they occur, syntactically,
    on `this`, then they occur in a method or constructor that is itself
    annotated as `@FromContract` (since the `caller()` is preserved in that case).
15. Bytecodes `jsr`, `ret` and `putstatic` are not used; inside constructors and instance
    methods, bytecodes `astore 0`, `istore 0`, `lstore 0`, `dstore 0` and
    `fstore 0` are not used.

> Local variable 0 is used to hold the `this` reference. Forbidding its modification
> is important to guarantee that `this` is not reassigned in code, which is impossible
> in Java but perfectly legal in (unexpected) Java bytecode.
> The guarantee that `this` is not reassigned is needed, in turn, for
> checking properties such as point 13 above.

16. There are no exception handlers that may catch
    unchecked exceptions (that is,
    instances of `java.lang.RuntimeException` or of `java.lang.Error`).

> By forbidding exception handlers for unchecked exceptions, it follows that
> unchecked exceptions will always make a transaction fail: all object
> updates up to the exception will be discarded. In practice,
> transactions failed because of an unchecked exception leave no trace on
> the store of the node, but for the gas of the caller being consumed. The reason for
> forbidding exception handlers for unchecked exceptions is that they could
> occur in unexpected places and leave a contract in an inconsistent state.
> Consider for instance the following (illegal) code:
> ```java
> try {
>   this.list.add(x);
>   x.flagAsInList();
>   this.counter++;
> }
> catch (Exception e) { // illegal in Takamaka
> }
> ```
> Here, the programmer might expect that
> the size of `this.list` is `this.counter` and the correctness of his code might
> be based on that invariant. However, if `x` holds
> `null`, an unchecked `NullPointerException` is raised just before
> `this.counter` could be incremented, it would be caught and ignored.
> The expected invariant would be lost.
> The contract will remain in blockchain in an inconsistent state,
> for ever. The situation would be worse if an `OutOfGasError` would
> be caught: the caller might provide exactly the amount of gas needed to
> reach the `flagAsInList()` call, and leave the contract in an inconsistent
> state. Checked exceptions, instead, are explicitly checked by the
> compiler, which should ring a bell in the head of the programmer.
>
> For a more dangerous example, consider the following Java bytecode:
>
> ```
> 10: goto 10
> exception handler for java.lang.Exception: 10 11 10 // illegal in Takamaka
> ```
>
> This Java bytecode exception handler entails that any `OutOfGasError`
> thrown by an instruction from line 10 (included) to line 11 (excluded)
> redirects control to line 10. Hence, this code will exhaust the gas by looping at line
> 10. Once all gas is consumed, an `OutOfGasError` is thrown, that is redirected
> to line 10. Hence another `OutOfGasError` will occur, that redirects the
> executor to line 10, again. And so on, for ever. That is, this code
> disables the guarantee that Takamaka transactions always terminate,
> possibly with an `OutOfGasError`. This code could be used for
> a DOS attack to a Hotmoka node. Although this code cannot be written in Java,
> it is well possible to write it directly, with a bytecode editor,
> and submit it to a Hotmoka node, that will reject it, thanks to point 16.

17. If a method or constructor is annotated as `@ThrowsException`, then it is public.
18. If a method is annotated as `@ThrowsException` and overrides another method,
    then the latter is annotated as `@ThrowsException` as well.
19. If a method is annotated as `@ThrowsException` and is overridden by another method,
    then the latter is annotated as `@ThrowsException` as well.
20. Classes installed in a node are not in packages `java.*`, `javax.*`
    or `io.takamaka.code.*`; packages starting with `io.takamaka.code.*` are
    however allowed if the node is not initialized yet.

> The goal of the previous constraint is to make it impossible to change
> the semantics of the Java or Takamaka runtime. For instance, it is not
> possible to replace class `io.takamaka.code.lang.Contract`, which could thoroughly
> revolutionize the execution of the contracts. During the initialization of a node,
> that occurs once at its start-up, it is however permitted to install the
> runtime of Takamaka (the `io-takamaka-code-1.5.0.jar` archive used in the examples
> in the previous chapters).

21. All referenced classes, constructors, methods and fields must be white-listed.
    Those from classes installed in the store of the node are always white-listed by
    default. Other classes loaded from the Java class path must have been explicitly
    marked as white-listed in the `io-hotmoka-whitelisting-1.9.0.jar` archive.

> Hence, for instance, the classes of the support library `io.takamaka.code.lang.Storage`
> and `io.takamaka.code.lang.Takamaka` are white-listed, since they
> are inside `io-takamaka-code-1.5.0.jar`, that is typically installed in the store of a
> node during its initialization. Classes from user
> jars installed in the node are similarly white-listed.
> Method `java.lang.System.currentTimeMillis()` is not white-listed,
> since it is loaded from the Java class path and is not annotated as white-listed
> in `io-takamaka-whitelisting-1.9.0.jar`.

22. Bootstrap methods for the `invokedynamic` bytecode use only standard call-site
    resolvers, namely, instances of `java.lang.invoke.LambdaMetafactory.metafactory`
    or of `java.lang.invoke.StringConcatFactory.makeConcatWithConstants`.

> This condition is needed since other call-site resolvers could call any
> method, depending on their algorithmic implementation, actually
> side-stepping the white-listing constraints imposed by point 22.
> Java compilers currently do not generate other call-site resolvers.

23. There are no native methods.
24. There are no `synchronized` methods, nor `synchronized` blocks.

> Takamaka code is single-threaded, to enforce its determinism.
> Hence, there is no need to use the `synchronized` keyword.

25. Field and method names do not start with a special prefix used
    for instrumentation, namely they do not start with `§`.

> This condition avoids name clashes after instrumentation.
> That prefix is not legal in Java, hence this constraint
> does not interfere with programmers. However, it could be used
> in (unexpected) Java bytecode, that would be rejected thanks to point 25.

26. Packages are not split across different jars in the classpath.

> This condition makes it impossible to call `protected` methods
> outside of subclasses and of the same jar where they are defined.
> Split packages allow an attacker to define a new jar
> with the same package name as classes in another jar and
> call the `protected` methods of objects of those classes.
> This is dangerous since `protected` methods
> often access or modify sensitive fields of the objects.

Takamaka verifies the following dynamic constraints:

1. Every `@Payable` constructor or method is passed a non-`null` and
   non-negative amount of funds.
2. A call to a `@Payable` constructor or method succeeds only if the caller
   has enough funds to pay for the call (ie., the amount first parameter of
   the method or constructor).
3. A call to a `@FromContract(C.class)` constructor or method succeeds only if
   the caller is an instance of `C`.
4. A bytecode instruction is executed only if there is enough gas for
   its execution.
5. Non-transient fields of type `java.lang.Object` or of type interface,
   belonging to some storage object reachable from the actual parameters of a transaction
   at the end of the transaction, contain `null` or a storage object.

## Command-Line Verification and Instrumentation

__[See `io-hotmoka-tutorial-examples-family_errors` in `https://github.com/Hotmoka/hotmoka`]__

If a jar being installed in a Hotmoka node does not satisfy the static
constraints that we have described before, the installation transaction fails with
a verification exception, no jar is actually installed but the gas of the
caller gets consumed. Hence it is not practical to realize that a
static constraint does not hold only by trying to install a jar in a node.
Instead, it is desirable to verify all constraints off-line, fix all
violations (if any) and only then install the jar in the node. This is
possible by using the `moka` command-line interface of Hotmoka.
Namely, it provides a subcommand that performs the same identical jar
verification that would be executed when a jar is
installed in a Hotmoka node.

Create a jar file containing
a wrong version of the `io-hotmoka-tutorial-examples-family` project. For that, copy the `io-hotmoka-tutorial-examples-family`
project into `io-hotmoka-tutorial-examples-family_errors` project, change the artifact name in its `pom.xml` into
`io-hotmoka-tutorial-examples-family_errors` and modify its `Person` class so that it contains
a few errors, as follows:

```java
package io.hotmoka.tutorial.examples.family;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.StringSupport;

@Exported
public class Person extends Storage {
  private final String name;
  private final int day;
  private final int month;
  private final int year;

  // error: arrays are not allowed in storage
  public final Person[] parents = new Person[2];

  public static int toStringCounter;

  public Person(String name, int day, int month, int year,
                Person parent1, Person parent2) {

    this.name = name;
    this.day = day;
    this.month = month;
    this.year = year;
    this.parents[0] = parent1;
    this.parents[1] = parent2;
  }

  // error: @Payable without @FromContract, missing amount and is not in Contract
  public @Payable Person(String name, int day, int month, int year) {
    this(name, day, month, year, null, null);
  }

  @Override
  public String toString() {
    toStringCounter++; // error: static update (putstatic) is not allowed
    return StringSupport.concat(name, " (", day, "/", month, "/", year, ")");
  }
}
```

Then generate the `io-hotmoka-tutorial-examples-family_errors-1.9.0` file:

```shell
$ cd io-hotmoka-tutorial-examples-family_errors
$ mvn install
```

Let us start with the verification of `io-takamaka-code-1.5.0.jar`,
taken from Maven's cache:

```shell
$ moka jars verify
    ~/.m2/repository/io/hotmoka/io-takamaka-code/1.5.0/io-takamaka-code-1.5.0.jar
    --init
Verification succeeded
```
No error has been issued, since the code does not violate any static constraint.
Note that we used the `--init` switch, since otherwise we would get many errors
related to the use of the forbidden `io.takamaka.code.*` package. With that
switch, we verify the jar as it would be verified before node initialization,
that is, by considering such package as legal.

We can generate the instrumented jar, exactly as it would be generated during
installation in a Hotmoka node. For that, we run:

```shell
$ mkdir instrumented
$ moka jars instrument
    ~/.m2/repository/io/hotmoka/io-takamaka-code/1.5.0/io-takamaka-code-1.5.0.jar
    instrumented/io-takamaka-code-1.5.0.jar
    --init
```

The `moka jars instrument` command verifies and instruments the jar, and then stores
its instrumented version inside the `instrumented` directory.

Let us verify and instrument `io-hotmoka-tutorial-examples-family-1.9.0.jar` now (the correct
version of the project).
As all Takamaka programs, it uses classes from the `io-takamaka-code` jar,
hence it depends on it. We specify this with the `--libs` option, that must
refer to an already instrumented jar:

```shell
$ moka jars instrument
    ~/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family/1.9.0/
        io-hotmoka-tutorial-examples-family-1.9.0.jar
    instrumented/io-hotmoka-tutorial-examples-family-1.9.0.jar
    --libs instrumented/io-takamaka-code-1.5.0.jar
```
Verification succeeds this time as well, and an instrumented `io-hotmoka-tutorial-examples-family-1.9.0.jar` appears in the
`instrumented` directory. Note that we have not used the `--init` switch this time, since we
wanted to simulate the verification as it would occur after the node has been already initialized,
when users add their jars to the store of the node.

Let us verify the `io-hotmoka-tutorial-examples-family_errors-1.9.0.jar` archive now, that
(we know) contains a few errors. This time, verification will fail and the errors will
be printed on the screen:
```shell
$ moka jars verify
    ~/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family_errors/1.9.0/
        io-hotmoka-tutorial-examples-family_errors-1.9.0.jar
    --libs instrumented/io-takamaka-code-1.5.0.jar 

Verification failed with the following errors:
1: io/hotmoka/tutorial/examples/family/Person.java field parents:
    type not allowed for a field of a storage class
2: io/hotmoka/tutorial/examples/family/Person.java method <init>: @Payable can only be applied
    to constructors or instance methods of a contract class or of an interface
3: io/hotmoka/tutorial/examples/family/Person.java method <init>: a @Payable method must have
    a first argument for the paid amount, of type int, long or BigInteger
4: io/hotmoka/tutorial/examples/family/Person.java method <init>: @Payable can only be applied
    to a @FromContract method or constructor
5: family/Person.java:56: static fields cannot be updated
```

The same failure occurs with the `moka jars instrument` command, that will not generate the instrumented jar. It only
reports the first encountered error before failure:
```shell
$ moka jars instrument
   ~/.m2/repository/io/hotmoka/io-hotmoka-tutorial-examples-family_errors/1.9.0/
       io-hotmoka-tutorial-examples-family_errors-1.9.0.jar
   instrumented/io-hotmoka-tutorial-examples-family_errors-1.9.0.jar
   --libs instrumented/io-takamaka-code-1.5.0.jar 

Instrumentation failed [io.hotmoka.verification.api.VerificationException:
  type not allowed for a field of a storage class]
```

# References

<a id="Antonopoulos17">[Antonopoulos17]</a>
Antonopoulos, A. M. (2017).
Mastering Bitcoin: Programming the Open Blockchain.
O'Reilly Media, 2nd edition.

<a id="AntonopoulosW19">[AntonopoulosW19]</a>
Antonopoulos, A. M. and Wood, G. (2019).
Mastering Ethereum: Building Smart Contracts and DApps.
O'Reilly Media.

<a id="AtzeiBC17">[AtzeiBC17]</a>
Atzei, N., Bartoletti, M. and Cimoli, T. (2017).
A Survey of Attacks on Ethereum Smart Contracts.
_6th Internal Conference on Principles of Security and Trust (POST17)_
ETAPS 2017.

<a id="BeniniGMS21">[BeniniGMS21]</a>
Benini, A., Gambini, M., Migliorini, S., Spoto, F. (2021).
Power and Pitfalls of Generic Smart Contracts.
_3rd International Conference on Blockchain Computing and Applications (BCCA2021)_.

<a id="BlindAuction">[BlindAuction]</a>
<a href="https://solidity.readthedocs.io/en/v0.5.9/solidity-by-example.html#id2">
https://solidity.readthedocs.io/en/v0.5.9/solidity-by-example.html#id2</a>.

<a id="CrafaPZ19">[CrafaPZ19]</a>
Crafa, S., Di Pirro, M. and Zucca, E. (2019).
Is Solidity Solid Enough?
_3rd Workshop on Trusted Smart Contracts (WTSC19)_.

<a id="CrosaraOST21">[CrosaraOST21]</a>
Crosara, M., Olivieri, L., Spoto, F. and Tagliaferro, F. (2021).
An Implementation in Java of ERC-20 with Efficient Snapshots.
_3rd International Conference on Blockchain Computing and Applications (BCCA2021)_.

<a id="EC2">[EC2]</a>
Amazon EC2: Secure and Resizable Compute Capacity in the Cloud.
<a href="https://aws.amazon.com/ec2">
https://aws.amazon.com/ec2</a>.

<a id="Freni20">[Freni20]</a>
Freni, P., Ferro, E. and Moncada, R. (2020).
Tokenization and Blockchain Tokens Classification: A Morphological Framework.
_IEEE Symposium on Computers and Communications (ISCC)_,
Rennes, France, pages 1-6.

<a id="IyerD08">[IyerD08]</a>
Iyer, K. and Dannen, C. (2018).
Building Games with Ethereum Smart Contracts: Intermediate Projects for Solidity Developers.
Apress.

<a id="JVM-Verification">[JVM-Verification]</a>
<a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.9">https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.9</a>.

<a id="LiskovW94">[LiskovW94]</a>
Liskov, B. and Wing, J. M. (1994).
A Behavioral Notion of Subtyping.
_ACM Transactions on Programming Languages and Systems_,
16(6):1811-1841.

<a id="MakB17">[MakB17]</a>
Mak, S. and Bakker, P. (2017).
Java 9 Modularity: Patterns and Practices for Developing Maintainable Applications.
Oreilly & Associates Inc.

<a id="Nakamoto08">[Nakamoto08]</a>
Nakamoto, S. (2008).
Bitcoin: A Peer-to-Peer Electronic Cash System.
Available at <a href="https://bitcoin.org/bitcoin.pdf">https://bitcoin.org/bitcoin.pdf</a>.

<a id="OliveiraZBS18">[OliveiraZBS18]</a>
Oliveira, L., Zavolokina, L., Bauer, I. and Schwabe, G. (2018).
To Token or not to Token: Tools for Understanding Blockchain Tokens.
_Proceedings of the International Conference on Information Systems - Bridging the Internet of People, Data, and Things, ICIS 2018_,
San Francisco, CA, USA,
Association for Information Systems.

<a id="OlivieriST21">[OlivieriST21]</a>
Olivieri, L., Spoto, F., Tagliaferro, F. (2021).
On-Chain Smart Contract Verification over Tendermint.
_5th Workshop on Trusted Smart Contracts (WTSC21)_.

<a id="Sentry">[Sentry]</a>
Sentry Node Architecture Overview - Cosmos Forum.
<a href="https://forum.cosmos.network/t/sentry-node-architecture-overview/454">
https://forum.cosmos.network/t/sentry-node-architecture-overview/454</a>.

<a id="Spoto19">[Spoto19]</a>
Spoto, F. (2019).
A Java Framework for Smart Contracts.
_3rd Workshop on Trusted Smart Contracts (WTSC19)_.

<a id="Spoto20">[Spoto20]</a>
Spoto, F. (2020).
Enforcing Determinism of Java Smart Contracts.
_4th Workshop on Trusted Smart Contracts (WTSC20)_.

<a id="Tapscott20">[Tapscott20]</a>
Tapscott, D. (2020).
Token Taxonomy: The Need for Open-Source Standards around Digital Assets.
<a href="https://www.blockchainresearchinstitute.org/project/token-taxonomy-the-need-for-open-source-standards-around-digital-assets">
https://www.blockchainresearchinstitute.org/project/token-taxonomy-the-need-for-open-source-standards-around-digital-assets</a>.

<a id="Tendermint">[Tendermint]</a>
<a href="https://tendermint.com">https://tendermint.com</a>.