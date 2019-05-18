# Takamaka: Smart Contracts in Java

Takamaka is a Java framework for writing smart contracts.
This tutorial explains how Takamaka code is written and
executed in blockchain.

# Table of Contents
1. [Introduction](#introduction)
2. [A First Takamaka Program](#first_program)

# Introduction <a name="introduction"></a>

Takamaka is a Java framework for writing smart contracts.
This means that it allows programmers to use Java for writing code
that can be installed and run on blockchain. Programmers will not have
to deal with the storage of objects in blockchain: this is completely
transparent to them. This makes Takamaka completely different from other
attempts at using Java for writing smart contracts, where programmers
must use specific method calls to persist data on blockchain.

Writing smart contracts in Java entails that programmers
do not have to learn yet another programming language.
Moreover, they can use a well-understood and stable development
platform, together with all its modern tools. Programmers can use
features from the latest versions of Java, such as streams and lambda
expressions.

There are, of course, limitations to the kind of code that can
be run inside a blockchain. The most important limitation is
deterministic behavior, as we will see later.

# A First Takamaka Program <a name="first_program"></a>

Let us start from a simple example of Takamaka code. Since we are
writing Java code, there is nothing special to learn or install
before starting writing programs in Takamaka. Just use your
preferred integrated development environment (IDE) for Java. Or even
do everything from command-line, if you prefer. Our examples below will be
shown for the Eclipse IDE.

Our goal will be to create a Java class that we will instantiate
and use in blockchain. Namely, we will learn how to create an object
of the class that will persist in blockchain and how we can later
call the `toString()` method on that instance in blockchain.

Let us hence create an Eclipse Java project `takamaka1`. Add
a `lib` folder inside it and copy there the two jars that contain the
Takamaka runtime and base development classes.
Add them both to the build path. The result should look
similar to the following:

![The `takamaka1` Eclipse project](pics/takamaka1.png "The takamaka1 Eclipse project")

Let us create a package `takamaka.tests.family`. Inside that package,
create a Java source `Person.java`, by copying and pasting
the following code:

```java
package takamaka.tests.family;

public class Person {
  private final String name;
  private final int day;
  private final int month;
  private final int year;
  public final Person parent1;
  public final Person parent2;

  public Person(String name, int day, int month, int year, Person parent1, Person parent2) {
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
    return name +" (" + day + "/" + month + "/" + year + ")";
  }
}
```

This is plain old Java code and should not need any comment. Compile it
(this should be automatic in Eclipse, if the Project &rarr; Build Automatically
option is set), create a folder `dist` and export there the project in jar format,
with name `takamaka1.jar` (click on the
`takamaka1` project, then right-click on the project, select Export &rarr; Java &rarr; Jar File
and choose the `dist` folder and the `takamaka1.jar` name). Only the compiled
class files will be relevant: Takamaka will ignore source files, manifest
and any resources in the jar, hence you needn't add them there. The result should
look as the following:

![The `takamaka1` Eclipse project, exported in jar](pics/takamaka1_jar.png "The takamaka1 Eclipse project, exported in jar")

The next step is to install that jar in blockchain, use it to create an instance
of `Person` and call `toString()` on that instance. For that, we need a running
blockchain node.

> Future versions of this document will show how to use a test network, instead of running a local simulation of a node.

Let us hence create another Eclipse project, that will start
a local simulation of a blockchain node, actually working over the disk memory
of our local machine. That blockchain simulation in memory is inside a third Takamaka jar.
Create then another Eclipse project named `blockchain`, add a `lib` folder and
include three Takamaka jars inside `lib`; both `takamaka_runtime.jar` and
`takamaka_memory.jar` must be added to the build path of this project;
do not add, instead, `takamaka_base.jar` to the build path: these base classes are
needed for developing Takamaka code (as shown before) and will be installed in blockchain
as a classpath needed by our running code. But they must not be part of the build path.
Finally, add inside `lib` and to the build path the BCEL jar that Takamaka uses for code instrumentation.
The result should look like the following:

![The `blockchain` Eclipse project](pics/blockchain1.png "The blockchain Eclipse project")

Let us write a main class that starts the blockchain in disk memory: create a package
`takamaka.tests.family` and add the following class `Main.java`:

```java
package takamaka.tests.family;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.TransactionException;
import takamaka.memory.InitializedMemoryBlockchain;

public class Main {
  public static void main(String[] args) throws IOException, TransactionException, CodeExecutionException {
    Blockchain blockchain = new InitializedMemoryBlockchain
      (Paths.get("lib/takamaka_base.jar"), BigInteger.valueOf(100_000), BigInteger.valueOf(200_000));
  }
}
```

As you can see, this class simply creates an instance of the blockchain on disk memory.
It requires to initialize that blockchain, by installing the base classes for Takamaka,
that we had previously put inside `lib`, and by creating two accounts, funded with
100,000 and 200,000 units of coin, respectively. We will use later such accounts
to run blockchain transactions. They will be available as `blockchain.account(0)`
and `blockchain.account(1)`, respectively.

So, what is the constructor of `InitializedMemoryBlockchain` doing here? Basically, it is
initializing a directory, named `chain`, and it is running a few initial transactions
that lead to the creation of two accounts. You can see the result if you run class
`takamaka.tests.family.Main`, refresh the `blockchain` project (click on it and push the F5 key)
and inspect the `chain` directory that should have appeared:

![The `chain` directory appeared](pics/blockchain2.png "The chain directory appeared")

Inside this `chain` directory, you can see that a block has been created (`b0`) inside which
four transactions (`t1`, `t2`, `t3` and `t4`) have been executed, that create and fund
our two initial accounts. Each transaction is specified by a request and a corresponding
response. They are kept in serialized form (`request` and `response`) but are also
reported in textual form (`request.txt` and `response.txt`). Such textual
representations would not be kept in a real blockchain, but are useful here, for debugging
or didactic purposes. We do not investigate further the content of the `chain` directory,
for now. Later, when we will run our own transactions, we will see these files in more detail. 