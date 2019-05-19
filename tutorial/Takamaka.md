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

import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.TransactionException;
import takamaka.memory.InitializedMemoryBlockchain;

public class Main {
  public static void main(String[] args) throws IOException, TransactionException, CodeExecutionException {
    InitializedMemoryBlockchain blockchain = new InitializedMemoryBlockchain
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
or learning purposes. We do not investigate further the content of the `chain` directory,
for now. Later, when we will run our own transactions, we will see these files in more detail.

Let us consider the `blockchain` project. The `Person` class is not in its build path
nor in its class path at run time.
If we want to call the constructor of `Person`, that class must somehow be in the class path.
In order to put `Person` in the class path, we must install
`takamaka1.jar` inside the blockchain, so that we can later refer to it and call
the constructor of `Person`. Let us hence modify the `takamaka.tests.family.Main.java`
file in order to run a transaction that install `takamaka1.jar` inside the blockchain:

```java
package takamaka.tests.family;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.request.JarStoreTransactionRequest;
import takamaka.memory.InitializedMemoryBlockchain;

public class Main {
  public static void main(String[] args) throws IOException, TransactionException, CodeExecutionException {
    InitializedMemoryBlockchain blockchain = new InitializedMemoryBlockchain
      (Paths.get("lib/takamaka_base.jar"), BigInteger.valueOf(100_000), BigInteger.valueOf(200_000));

    TransactionReference takamaka1 = blockchain.addJarStoreTransaction(new JarStoreTransactionRequest(
      blockchain.account(0), // this account pays for the transaction
      BigInteger.valueOf(100_000L), // gas provided to the transaction
      blockchain.takamakaBase, // reference to a jar in the blockchain that includes the basic Takamaka classes
      Files.readAllBytes(Paths.get("../takamaka1/dist/takamaka1.jar")), // bytes containing the jar to install
      blockchain.takamakaBase // dependency
    ));
  }
}
```

The `addJarStoreTransaction()` method expands the blockchain with a new transaction, whose goal
is to install a jar inside the blockchain. The jar is provided as a sequence of bytes
(`Files.readAllBytes(Paths.get("../takamaka1/dist/takamaka1.jar"))`, assuming that the
`takamaka1` project is in the same workspace as `blockchain`). This transaction, as any
Takamaka transaction, must be payed. The payer is specified as `blockchain.account(0)`, that is,
the first of the two accounts created at the moment of creation of the blockchain.
It is specified that the transaction can cost up to 100,000 units of gas. The transaction request
specifies that its class path is `blockchain.takamakaBase`: this is the reference to a jar
installed in the blockchain at its creation time and containing `takamaka_base.jar`, that is,
the basic classes of Takamaka. Finally, the request specifies that `takamaka1.jar` has only
a single dependency: `takamaka_base.jar`. This means that when, below, we will refer to
`takamaka1` in a class path, this will indirectly include its dependency `takamaka_base.jar`.

Run the `Main` class again, refresh the `blockchain` project and see that the `chain` directory
is one transaction longer now:

![A new transaction appeared in the `chain` directory](pics/blockchain3.png "A new transaction appeared in the chain directory")

The new `t4` transaction reports a `request` that corresponds to the request that we have
coded in the `Main` class. Namely, its textual representation `request.txt` is:

```
JarStoreTransactionRequest:
  caller: 0.2#0
  gas: 100000
  class path: 0.0 non-recursively resolved
  dependencies: [0.0 non-recursively resolved]
  jar: 504b0304140008080800d294b24e000000000000000000000000140004004d4554412d494e462f4d414e49464553542e4d46f...
```

The interesting point here is that objects, such as the caller account
`blockchain.account(0)`, are represented as _storage references_ such as `0.2#0`. You can
see a storage reference as a machine-independent, deterministic pointer to an object contained
in the blockchain. Also the `takamaka_base.jar` is represented with an internal representation.
Namely, `0.0` is a _transaction reference_, that is, a reference to the transaction that installed
`takamaka_base.jar` in the blockchain: transaction 0 of block 0. The jar is the hexadecimal
representation of its byte sequence.

Let us have a look at the `response.txt` file, which is the textual representation of the outcome of
the transaction:

```
JarStoreTransactionSuccessfulResponse:
  consumed gas: 1258
  updates:
    <0.2#0|takamaka.lang.Contract.balance:java.math.BigInteger|99874>
  instrumented jar: 504b03041400080808007ca3b24e0000000000000000000000002200040074616b616d616b612f74657374732f66616d696c792...
```

The first bit of information tells us that the transaction costed 1,258 units of gas. We had accepted to spend up to
100,000 units of gas, hence the transaction could complete correctly. The response reports also the hexadecimal representation
of a jar, which is no named _instrumented_. This is because what gets installed in blockchain is not exactly the jar sent
with the transaction request, but an instrumentation of that, which adds specific features that are specific to Takamaka code.
For instance, the instrumented code will charge gas during its execution. Finally, the response reports _updates_. These are
state changes occured during the execution of the transaction. In order terms, updates are the side-effects of the transaction,
i.e., the fields of the objects modified by the transaction. In this case, the balance of the payer of the transaction
`0.2#0` has been reduced to 99,874, since it payed for the gas (we created that account with 100,000 coin units at its
beginning).

> The actual amount of gas consumed by this transaction might change in future versions of Takamaka.

We are now in condition to call the constructor of `Person` and create an instance of that class in blockchain.
First of all, we must create the class path where the constructor will run. Since the class `Person` is inside
the `takamaka1.jar` archive, the class path is simply:

```java
Classpath classpath = new Classpath(takamaka1, true);
```

The `true` flag at the end means that this class path includes the dependencies of `takamaka1`. If you look
at the code above, where `takamaka1` was defined, you see that this means that the class path will include
also the dependency `takamaka_base.jar`. If `false` would be used instead, the class path would only include
the classes in `takamaka1.jar`, which would be a problem when we will use, very soon, some support classes that
Takamaka provides, in `takamaka_base.jar`, to simplify the life of developers.

Clarified which class path to use, let us trigger a transaction that runs the constructor and adds the brand
new `Person` object into blockchain. For that, modify the `takamaka.tests.family.Main.java` source as follows:

```java
package takamaka.tests.family;

import static takamaka.blockchain.types.BasicTypes.INT;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import takamaka.blockchain.Classpath;
import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.ConstructorSignature;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.request.ConstructorCallTransactionRequest;
import takamaka.blockchain.request.JarStoreTransactionRequest;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.IntValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StringValue;
import takamaka.memory.InitializedMemoryBlockchain;

public class Main {
  // useful constants
  private final static BigInteger _100_000 = BigInteger.valueOf(100_000L);
  private final static ClassType PERSON = new ClassType("takamaka.tests.family.Person");

  public static void main(String[] args) throws IOException, TransactionException, CodeExecutionException {
    InitializedMemoryBlockchain blockchain = new InitializedMemoryBlockchain
      (Paths.get("lib/takamaka_base.jar"), BigInteger.valueOf(100_000), BigInteger.valueOf(200_000));

    TransactionReference takamaka1 = blockchain.addJarStoreTransaction(new JarStoreTransactionRequest(
      blockchain.account(0), // this account pays for the transaction
      _100_000, // gas provided to the transaction
      blockchain.takamakaBase, // reference to a jar in the blockchain that includes the basic Takamaka classes
      Files.readAllBytes(Paths.get("../takamaka1/dist/takamaka1.jar")), // bytes containing the jar to install
      blockchain.takamakaBase
    ));

    Classpath classpath = new Classpath(takamaka1, true);

    StorageReference albert = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest(
      blockchain.account(0), // this account pays for the transaction
      _100_000, // gas provided to the transaction
      classpath, // reference to takamaka1.jar anbd its dependency takamaka_base.jar
      new ConstructorSignature(PERSON, ClassType.STRING, INT, INT, INT), // constructor Person(String,int,int,int)
      new StringValue("Albert Einstein"), new IntValue(14), new IntValue(4), new IntValue(1879) // actual arguments
    ));
  }
}
```

The `addConstructorCallTransaction()` method expands the blockchain with a new transaction that calls
a constructor. Again, we use `blockchain.account(0)` to pay for the transaction and we provide
100,000 units of gas, which should be enough for a constructor that just initializes a few fields.
The class path includes `takamaka1.jar` and its dependency `takamaka_base.jar`, although the latter
is not used yet. The signature of the constructor specifies that we are referring to the second
constructor of `Person`, the one that assumes `null` as parents. Finally, the actual parameters
are provided; they must be instances of the `takamaka.blockchain.values.StorageValue` interface.

Let us run the `Main` class. The result is disappointing:

```
Exception in thread "main" takamaka.blockchain.TransactionException: Failed transaction
    at takamaka.blockchain.AbstractBlockchain.wrapAsTransactionException(Unknown Source)
    at takamaka.blockchain.AbstractBlockchain.lambda$runConstructorCallTransaction$11(Unknown Source)
    at takamaka.blockchain.AbstractBlockchain.wrapInCaseOfException(Unknown Source)
    at takamaka.blockchain.AbstractBlockchain.runConstructorCallTransaction(Unknown Source)
    at takamaka.blockchain.AbstractBlockchain.lambda$addConstructorCallTransaction$12(Unknown Source)
    at takamaka.blockchain.AbstractBlockchain.wrapWithCodeInCaseOfException(Unknown Source)
    at takamaka.blockchain.AbstractBlockchain.addConstructorCallTransaction(Unknown Source)
    at takamaka.tests.family.Main.main(Main.java:42)
Caused by: java.lang.ClassCastException: takamaka.tests.family.Person cannot be cast to takamaka.lang.Storage
    at takamaka.blockchain.AbstractBlockchain$ConstructorExecutor.run(Unknown Source)
```

> The exact shape and line numbers of this exception trace might change in future versions of Takamaka.

The transaction failed. Nevertheless, a transaction has been added to the blockchain: refresh the
`chain` folder and look at the topmost transaction `chain/b1/to`. There is a `request.txt`, that contains
the information that we provided in the `addConstructorCallTransaction()` specification, and there is
a `response.txt` that contains the (disappointing) outcome:

```
ConstructorCallTransactionFailedResponse:
  consumed gas: 100000
  updates:
    <0.2#0|takamaka.lang.Contract.balance:java.math.BigInteger|88347>
```

Note that the transaction costed a lot: all 100,000 gas units have been consumed! This is a sort
of punishment for running a transaction that fails. The rationale is that this punishment should
discourage potential denial-of-service attacks, when a huge number of failing transactions are thrown
at a blockchain. At least, this attack will cost a lot.

But we still have not understood why the transaction failed. The reason is in the exception
message: `takamaka.tests.family.Person cannot be cast to takamaka.lang.Storage`. Takamaka rerquires
that all objects stored in blockchain extends the `takamaka.lang.Storage` class. That superclass
provides all the machinery needed in order to keep track of updates to such objects.

> Do not get confused here. Takamaka does **not** require all objects to extend
> `takamaka.lang.Storage`. You can use objects that do not extend that superclass in your
> Takamaka code, both instances of your classes and instances of library classes
> from the `java.*` hierarchy, for instance. What Takamaka does require, instead, is that objects
> _that must be kept in blockchain_ do implement `takamaka.lang.Storage`. This is the
> case, for instance, of objects created by the constructor invoked through the
> `addConstructorCallTransaction()` method.

Let us modify the `takamaka.tests.family.Person.java` source code then:

```java
package takamaka.tests.family;

import takamaka.lang.Storage;

public class Person extends Storage {
  ... unchanged code ...
}
```

> Extending `takamaka.lang.Storage` is all a programmer needs to do in order to let instances
> of a class be stored in blockchain. There is no explicit method to call to keep track
> of updates to such objects: Takamaka will automatically deal with the updates.

Regenerate `takamaka1.jar`, since class `Person` has changed, and export it again as
`dist/takamaka1.jar`, inside the `takamaka1` Eclipse project (some versions of Eclipse
require to delete the previous `dist/takamaka1.jar` before exporting a new version).
Run again the `takamaka.tests.family.Main` class.

> We can use the `takamaka.lang.Storage` class and we can run the resulting compiled code
> since that class is inside `takamaka_base.jar`, which as been included in the
> class path as a dependency of `takamaka1.jar`.

This time, the execution should
complete without exception. Refresh the `chain/b1/t0` directory and look at the
`response.txt` file. This time the transaction was succesful:

```
ConstructorCallTransactionSuccessfulResponse:
  consumed gas: 130
  updates:
    <THIS_TRANSACTION#0.class|takamaka.tests.family.Person>
    <0.2#0|takamaka.lang.Contract.balance:java.math.BigInteger|98334>
    <THIS_TRANSACTION#0|takamaka.tests.family.Person.day:int|14>
    <THIS_TRANSACTION#0|takamaka.tests.family.Person.month:int|4>
    <THIS_TRANSACTION#0|takamaka.tests.family.Person.year:int|1879>
    <THIS_TRANSACTION#0|takamaka.tests.family.Person.name:java.lang.String|Albert Einstein>
    <THIS_TRANSACTION#0|takamaka.tests.family.Person.parent1:takamaka.tests.family.Person|null>
    <THIS_TRANSACTION#0|takamaka.tests.family.Person.parent2:takamaka.tests.family.Person|null>
  new object: THIS_TRANSACTION#0
  events:
```

You do not need to understand the content of this response file in order to program
in Takamaka. However, it can be interesting to get an idea of its content.
The file tells that a new object has been created and stored in blockchain. It is identified as
`THIS_TRANSACTION#0` since it is the first (0th) object created during this transaction.
Its class is `takamaka.tests.family.Person`:

```
<THIS_TRANSACTION#0.class|takamaka.tests.family.Person>
```

and its fields are initialized as required:

```
<THIS_TRANSACTION#0|takamaka.tests.family.Person.day:int|14>
<THIS_TRANSACTION#0|takamaka.tests.family.Person.month:int|4>
<THIS_TRANSACTION#0|takamaka.tests.family.Person.year:int|1879>
<THIS_TRANSACTION#0|takamaka.tests.family.Person.name:java.lang.String|Albert Einstein>
<THIS_TRANSACTION#0|takamaka.tests.family.Person.parent1:takamaka.tests.family.Person|null>
<THIS_TRANSACTION#0|takamaka.tests.family.Person.parent2:takamaka.tests.family.Person|null>
```

The account that payed for the transaction sees its balance decrease:

```
<0.2#0|takamaka.lang.Contract.balance:java.math.BigInteger|98334>
```

These triples are called _updates_, since they describe how the blockchain was
updated to cope with the creation of a new object.

So where is this new `Person` object, actually? Well, it exists in blockchain only.
It did exist in RAM during the execution of the constructor. But, at the end
of the constructor,
it was deallocated from RAM and serialized in blockchain, as a set of updates.
Its storage reference has been returned to the caller of
`addConstructorCallTransaction()`:

```java
StorageReference albert = blockchain.addConstructorCallTransaction(...)
```

and can be used later to invoke methods on the object or to pass the object
as a parameter of methods or constructors: when that will occur, the object
will be deserialized from its updates in blockchain and recreated in RAM.