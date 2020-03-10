# `it-univr-bcel`
BCEL utilities for inferring types in Java bytecode and for recomputing stack maps for a method.
A stack map is compulsory from Java bytecode version 50 and higher. It specifies types at specific
program points, ie., at targets of jumps. Compilers generate code with stack map tables, but code instrumentation
might invalidate them. These utilities allow one to recompute stack map tables after code instrumentation with
the [BCEL library for bytecode instrumentation](https://commons.apache.org/proper/commons-bcel/).

## Type Inference
Given a BCEL `MethodGen` named `m`, this library allows one to infer the types at each of its instruction by computing

```java
TypeInferrer inferrer = TypeInferrer.of(m);
```

Types at each instruction handle `ih` in `m` can then be accessed by using an intuitive API, as in the following example:

```java
Types types = inferrer.getTypesAt(ih);
int stackHeight = types.stackHeight();
int localsCount = types.localsCount();
Type top = types.getStack(stackHeight - 1);
Type local0 = types.getLocal(0);
```
## Stack-Map Inference
Given a BCEL `MethodGen` named `m` (with or without a stack map), this library allows one to infer a new stack map for `m`
by computing

```java
StackMapInferrer inferrer = StackMapInferrer.of(m);
Optional<StackMap> = inferrer.getStackMap();
```

The result is `Optional` since not all methods need a stack map.

## Stack-Map Recomputation
Given a BCEL `MethodGen` named `m` (with or without a stack map), this library allows one to modify `m` by removing its
old stack map (if any) and adding a new inferred stack map for it (if needed), by computing:

```java
StackMapReplacer.of(m);
```
