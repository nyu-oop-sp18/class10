# Class 10

## Overloading Resolution

Like many other languages, Scala supports overloading methods. That
is, it can distinguish between methods with the same name but
different signatures. This means that methods within a class can have
the same name if they have different parameter lists.

Overloaded methods are disambiguated by parameter list *type* and *arity*

  * *type* is a classification identifying one of various types of data,
    such as integer or Boolean.

  * *arity* refers to the number of arguments or operands a function
    or operation takes.

Overloading vs. overriding:

  * When overloading a method, you are really just declaring a number
    of different methods that happen to have the same name. Which
    version of an overloaded method is called at a given call site is
    determined statically at compile-time based on 1) the *static
    type* of the receiver object of the called method, and 2) the
    *static types* of the arguments to the called method.

  * This should not be confused with overriding where the correct
    method is chosen at run-time using the *dynamic type* of the
    receiver object (which is implemented using dynamic dispatch via a
    vtable).

  * That is, think of overloading resolution as determining which
    vtable slot to use for the lookup involved during dynamic dispatch
    of overridden methods.

Caveats:

  * You cannot declare more than one method with the same name and the
    same number and type of arguments, because the compiler cannot
    tell them apart.

  * The compiler does not consider return type when differentiating
    methods, so you cannot declare two methods with the same signature
    but with different return types.

  * Overuse of overloading can make code less readable.

We will focus on Scala's overloading resolution rules here, but most
languages that support overloading use similar rules.
  
### Example

Consider the following Scala object which provides three classes with
an overloaded method `m`:

```scala
object Overloading extends App {

  class A {
    def m(x: A): A = { println("A.m(A)"); new B() }
    def m(x: A, y: A): Unit = { println("A.m(A, A)") }
    def m(x: A, y: B): Unit = { println("A.m(A, B)") }
    def m(x: B, y: A): Unit = { println("A.m(B, A)") }
    def m(x: C, y: C): Unit = { println("A.m(C, C)") }
    def m(x: String): Unit = { println("A.m(String)") }
    def m(x: Any): Unit = { println("A.m(Any)") }
    def m(x: Short): Unit = { println("A.m(Short)") }
    def m(x: Int): Unit = { println("A.m(Int)") }
    def m(x: Long): Unit = { println("A.m(Long)") }
    def m(x: Double): Unit = { println("A.m(Double)") }
  }
  
  class B extends A {
    override def m(x: A): A = { println("B.m(A)"); new A() }
    def m(x: B): Unit = { println("B.m(B)") }
  }
  
  class C extends B
}
```

Let's consider the following declarations

```scala
val a = new A()
val b = new B()
val c = new C()
val s1: Short = 1
val s2: Short = 1
```

Now, for each of the following calls to `m`, we want to determine
which version of `m` is called:
  
  * `a.m(a)`
  
    * We first determine the static type of the receiver object `a` to
      determine where to search for candidate methods for the
      resolution.
      
    * The type of `a` is `A`. Thus, we look for candidates in `A` and
      its subclasses/traits that are accessible at the call site.
  
    * Next, we analyze the static types of the arguments to the call,
      here `a`. The type is again `A`, so we are looking for a method
      that expects an `A` as argument. The method `A.m(A)` fits the
      bill, so the vtable slot of this method is being used for the
      dynamic dispatch.
      
    * Since the dynamic type of `a` is also `A`. The method `A.m(A)`
      is being called at run-time.

  * `val a: A = new B(); a.m(a);`

    * In this case, the result of the overloading resolution does not
      change as the static type of `a` is still `A`. That is, we will
      still use the vtable slot for `A.m(A)` for the dynamic
      dispatch. However, since now the dynamic type of `a` is `B` and
      `B` overrides `A.m(A)`, we will be calling `B.m(A)` at run-time.

  * `a.m(s1);` 
  
     * Calling with one `Short` as argument on an `A` behaves in the
       obvious way, dispatching to `A.m(Short)`.
        
  * `a.m(s1 + s2);`
  
     * With the sum of two `Short`s as one parameter, we call `A.m(Int)`
       because in Scala, the sum of two `Short`s has type `Int`.
       
     * This is because of the JVM design: Java instructions are only 8
       bits wide, so there is not enough space to allow for separate
       arithmetic operations for bytes, shorts, ints, longs, floats,
       and doubles.
       
     * With only 256 total instructions, `Short` addition was considered
       not important enough to justify having its own
       instruction. This is an example where the JVM architecture
       has affected the language design.
        
  * ```scala
    a.m(new AnyRef())
    a.m(new Exception()) 
    a.m("String")
    ```
    
    * Calling `m` with an `AnyRef`, an `Exception`, or a `String` also behaves
      just as expected.

    * In particular, if we comment out the method `m(String)`, passing
      a `String` will call `A.m(Any)`. If we uncomment it, then we
      call `A.m(String)`.

  * ```scala
    b.m(a)
    b.m(b)
    b.m(c)
    ```
  
    * Now with inheritance: calling `b.m(a)`, `b.m(b)`, `b.m(c)` is resolved to
      `B.m(A)`, `B.m(B)`, `B.m(B)`, respectively, because we invoke the most
      specific match. That is, we have `C` extends `B` extends `A` but we
      only have distinct `m` methods for `A` and `B` in `B`.
        
    * Similarly, the calls `a.m(b)` and `a.m(c)` will go to `A.m(a)`.
        
  * ```scala
    a.m(a, a)
    a.m(c, c)
    ```
    
    * The same logic holds for `a.m(a,a)` and `a.m(c,c)`, which calls `A.m(A,A)`
      and `A.m(C,C)` because there is a direct match.
    
* Tricky points

  * `a.m(b, b)`
    
    * For `a.m(b,b)` it is less clear because we only have `A.m(A,B)` and
      `A.m(B,A)` and neither is more specific than the other
      (intuitively; see a more formal notion of a method's generality
      below)

    * The code does not compile because the compiler does not like the
      ambiguity in the method call.
          
  * `a.m((b: A), b)`
    
    * We can resolve the ambiguity with explicit casting of one of the
      arguments to an `A`, and now our code compiles.

    * Since `B extends A`, the cast is an upcast and always safe,
      i.e., requires no run-time checking. The cast is only used for
      resolution of overloading.
    
* Implicit conversions

  * When trying to find a matching overloaded variant of a called
    method the compiler first tries to find the best fitting method
    by following the subtype relation upwards for the receiver and
    argument expressions.
    
  * If no matching method can be identified, the compiler tries to
    make the receiver and argument fit by using implicit conversion
    functions that are in scope of the method call.
    
  * For instance, suppose we have the following implicit conversion
    from `String` to `A` in `Overloading`:
    
    ```scala
    implicit def AfromString(s: String): A = new A()
    ```
    
    Then for the call
    
    ```scala
    "Hello".m("World")
    ```
    
    The compiler will first search for `m` methods in `String` and its
    superclasses. Since no candidate methods exist, the compiler will
    consider the implicit conversion of the receiver `"Hello"` to `A`
    using `AfromString` and search for candidates in `A`. The method
    `A.m(String)` fits, so the actual call will be
    
    ```scala
    AfromString("Hello").m("World")
    ```
    
    and go to `A.m(String)`.
    
    If we comment out `A.m(String)`, the call will got to `A.m(Any)`
    instead. If we also comment out `A.m(Any)`, the compiler will next
    try implicit conversions on the arguments as well. So in that
    case, the call would end up looking like this:
    
    ```scala
    AfromString("Hello").m(AfromString("World"))
    ```
    
    and go to `A.m(A)`.
    
    The compiler will consider at most one application of an implicit
    conversion function per receiver/argument. Also, as in the case of
    subtyping if there is an ambiguity between different possible
    conversion functions that can be applied to find matching methods,
    the compiler will produce an error.
    
### Rules for Resolution of Overloading

How is a call to an overloaded method of the form `e1.m(e2, ..., em)`
resolved?

* First determine the class or trait where to start searching for
  candidate methods. This is done via the static type `T1` of the
  receiver object `e1`. Then find methods `m` that are applicable and
  accessible within `T1`.

* Now we need to determine the static types of the argument
  expressions `e1, ..., en` so that we can decide which methods are
  applicable (same number of arguments as parameters, each of correct
  type or can be statically cast to correct type).
    
* We also need to make sure that we only use accessible methods: 

  * do not attempt to access an instance private method from
    outside the body of the class where it is declared and through any
    receiver other than `this`.

  * do not attempt to access private methods from outside the body of
    the class `C` where it is declared unless it is `C`'s companion
    object.
  
  * do not attempt to access a package private method from outside the
    package where it is declared.

  * do not attempt to access a protected method of a class `C` from
    a scope that is not a subclass of `C` or the companion object of a
    subclass of `C`.
  

* If more than one method declaration is both accessible and
  applicable, choose the most specific method.
  
* One method declaration is more specific than another if any
  invocation handled by the first can be passed on to the other
  without a compiler error.
    
* So to return to our example from `Overloading`, neither `A.m(A, B)` nor
  `A.m(B, A)` is more specific than the other. This is why we
  encountered an ambiguity when attempting to call `a.m(b, b)`.
    
* There is also a concept of generality which applies to numeric value
  types: numeric types are "more specific" than object types
  
  * e.g. `Int` goes to `Long` which goes to `Double` before being
    auto-boxed to `AnyVal`, which extends `Any`.
    
  * Scala inherits the corresponding widening, shortening, and
    coercion rules for numeric types from the JVM. They are enumerated
    in the Java language specification.

* If no matching method is found in `T1`, then proceed with the search
  in `T1`'s parent class or trait as above. If `T1` is the result of a
  mixin composition, the immediate parent of `T1` is the rightmost
  trait preceeding `T1` in the mixin chain.

## Object Initialization

The initialization of class and object members in OOP languages is
notoriously complex. For Java and other JVM-based languages, we can
refer to the Java Language Specification to figure out how things
work.

* The 8th edition of the JLS gives semantics to Java 8 programs

  * The document has 788 pages.

  * About 120 pages to define semantics of expressions.

  * More than 20 pages related to class and object initialization.
  
Much of the complexity in initialization arises from language features
such as dynamic class loading and concurrency.

For example, what does the following program print?

```scala
object Loopy extends App {

  object A {
    val x: Int = B.x + 1
  }

  object B {
    val x: Int = A.x + 1
  }

  println(s"B: ${B.x}, A: ${A.x}")
  
}
```


If we run class `Loopy`:
  
1. The the `println` call in `Loopy` first accesses `A.x`.
1. The class generated for object `A` is loaded and all its static
   fields are default initialized (i.e., `A.x` is now 0 according to
   section 4.12.5 of JLS).
1. The lock for `A` is taken by the current thread.
1. Static initializer of `A` runs and accesses `B.x` when
   initializing `A.x`.
1. The classed generated for object `B` is loaded and all its static
   fields are default initialized (i.e., `B.x` is now 0).
1. The lock for `B` is taken by the current thread.
1. Static initializer of `B` runs and accesses `A.x` when
   initializing `B.x`.
1. Class `A` is still locked by current thread. Recursive
   initialization is detected (see section 12.4 of JLS). Therefore,
   initialization returns immediately.
1. The value of `A.x` is still 0 (see sections 12.3.2 and 4.12.5 of
   JLS), so `B.x` is set to 1.
1. Initialization of `B` finishes and the lock on `B` is released.
1. The value of `A.x` is now set to `2`.
1. Initialization of `A` finishes and the lock on `A` is released.
1. The program prints `"A: 2, B: 1"`.

Note that the resulting values of `A.x` and `B.x` depend on the order
in which the corresponding classes are loaded at run-time. E.g. if we
swap `A.x` and `B.x` in the `println` call, we will have that `B.x` is
`2` and `A.x` is `1`. In general, it is therefore not possible to tell
statically at compile-time what values static fields (i.e. fields of
companion objects in Scala) will be initialized to.

The situation is slightly better when analyzing the initialization of
instances of classes, which we discuss next.

### Instance Initialization in Scala

* In general, whenever a new object instance `o` of some class `C` is
  created via an expression `new C(a1,...,an)`, the object is
  initialized in two steps:

  1. All fields of `o` are default initialized (e.g., fields of type
     `Int` are set to `0`, fields of reference types are set to
     `null`, etc.)

  1. Then, the *init method* `<init>(T1, ..., Tn)` for the constructor
     with signature `C(T1, ..., Tn)` is executed. Which constructor is
     called is determined by overloading resolution similar to regular
     method calls.
  
* Each constructor of a class is compiled to an associated init
  method. Recall that every Scala class has a primary constructor that
  is implicitly defined by the parameter list of the class. E.g. if we
  have a class declaration of the form
  
  ```scala
  class C(x1: T1, ..., xn: Tn) extends B(e1, ..., em) { ... }
  ```
  
  then we obtain a primary constructor with the signature `C(x1: T1,
  ..., xn: Tn)`
  
* The init method of the primary constructor `C(x1: T1, ..., xn: Tn)`
  consists of several parts:

  1. First, if any of the parameters `xi` in the class parameter list
     is declared as a field of the class using `val` or `var`, then
     that field is immediately initialized with the value passed to
     `xi` in the init method.

  1. Next, the init method of some constructor of `C`s superclass `B`
     is called. Which constructor is used for this call is determined
     by overloading resolution of the `extends` expression `B(e1, ..., em)`.

  1. Next, the code of all top-level expressions in the body of class
     `C` as well as field initialization expressions of any field
     declaration in `C` are executed. These are executed in the same
     order as they appear in the source code of the class declaration.
  
  
For a complete example, consider the following Scala classes:

```scala
class Base(val x: Int) {
  val y = 0
  
  println(y)
  
}
```

We can inspect the generated init methods for the primary constructors
of `Base` and `Derived` by decompiling the Java bytecode generated by
the Scala compiler for the two classes.

Running the command

```
javap -c target/scala-2.12/classes/Base.class
```

will print the following disassembled bytecode for the implementation
of `Base`'s primary constructor:

```java
public Base(int);
    Code:
       0: aload_0
       1: iload_1
       2: putfield      #14                 // Field x:I
       5: aload_0
       6: invokespecial #23                 // Method java/lang/Object."<init>":()V
       9: aload_0
      10: iconst_0
      11: putfield      #18                 // Field y:I
      14: getstatic     #29                 // Field scala/Predef$.MODULE$:Lscala/Predef$;
      17: aload_0
      18: invokevirtual #31                 // Method y:()I
      21: invokestatic  #37                 // Method scala/runtime/BoxesRunTime.boxToInteger:(I)Ljava/lang/Integer;
      24: invokevirtual #41                 // Method scala/Predef$.println:(Ljava/lang/Object;)V
      27: return
}
```

* Instructions 0-2 correspond to the initialization of field `Base.x` using
  the value passed to parameter of the init method (loaded with
  `iload_1`).
  
* Instructions 5 and 6 implement the call to the superclass init
  method. In our example, `Base` extends `AnyRef` which is mapped to
  `java.lang.Object` by the compiler.

* Next, the initialization code in the body of `Base` is executed in
  order of appearance: instructions 9-11 initialize `Base.y` with
  `0`. The remaining instructions 14-27 encode the call `println(y)`.
  
Note that the method `y` is accessed indirectly via a call to an
instance method of the same name. For every instance method of a
class, the compiler will automatically generate a *getter* method of
the same name in the class. Whenever the field is accessed, the access
will be translated to a call to the getter method. This indirect
access mechanism is used to implement overriding of `val` fields in
subclasses. Instead of overriding the actual field, the subclass
overrides the getter method with a new implementation that returns the
value of the new field. Accesses to the field in the code of the
superclass on subclass instances will then be dispatched to the new
getter method which returns the new value of the field.

Here is an example that shows this in action:

```scala
object Quiz extends App {
  trait A {
    val foo: Int
    val bar = 1
    println("In A: foo: " + foo + ", bar: " + bar)
  }

  class B extends A {
    val foo: Int = 42
    println("In B: foo: " + foo + ", bar: " + bar)
  }

  class C extends B {
    override val bar = 99
    println("In C: foo: " + foo + ", bar: " + bar)
  }

  new C

}
```

This code will print:

```
In A: foo: 0, bar: 0
In B: foo: 42, bar: 0
In C: foo: 42, bar: 99
```

The explanation is as follows:

1. When the `C` instance is created, all its fields are first 0
   initialized.
   
1. Then the init method of `C` is called, which in turn calls the init
   method of `B`, which in turn calls the init method of `A`.
   
1. The init method of `A` then sets the field `A.bar` to `1` and
   executes the `println` call. The access to `foo` calls the
   corresponding getter method and retrieves the value `0` as the
   field `foo` has only been default initialzed so far. The access to
   `bar` likewise calls the getter method for `bar`. However, since
   `bar` is overridden in `C`, this call is dispatched to the new
   version of the getter method provided by `C`, which will read the
   value of the new `bar` field in `C`. This field has only been
   default initialized so far, so we obtain value `0` as well.
   
1. Next, the init method of `A` completes and the init method of `B`
   continues executing the initialization code from the body of
   `B`. It first initializes field `foo` to `42` and then executes the
   `println` call in `B`. The calls to the getter methods retrieve the
   new value for `foo` and the default value for `bar` as the field of
   the overridden `bar` has still only been default initialized.

1. Finally, the init method of `B` completes and the init method of
   `C` completes its initialization by setting the overriding `bar`
   field to `99` and executing the `println` call in `C`. This call
   now prints values `42` and `99` accordingly.


## Call by Name and Lazy Evaluation

In programming language semantics we distinguish between different
parameter passing modes for function/method calls, most notably the
*call-by-value* and *call-by-name* modes. In call-by-value parameter
passing, the arguments to methods calls are evaluated before the call
is executed and only the values are passed to the method. Thus, each
argument is evaluated exactly once before the call. On the other hand,
for a call-by-name parameter `x`, the argument expression passed to
`x` is not evaluated immediately at the call site before the call is
executed. Instead, during the execution of the method body, each time
the value of `x` is used, the argument expression is reevaluated.

By default, Scala treats all parameters using call-by-value
semantics. However, a parameter can be declared as pass-by-name by
putting an arrow in front of the parameter type:

```scala
def f(x: => T) e
```

If we ignore the effect on the running time for evaluating arguments
multiple times (respectively, not at all), the difference between
call-by-value and call-by-name parameter semantics can only be
observed if the evaluation of the argument expression has side
effects. So let's consider a simple example that involves a
call-by-name parameter and an argument expression with side effects:

```scala
def f(x: => Int) {
  println(x)
  println(x)
}

var y = 0

f({y = y + 1; y})
```

The expression block `{y = y + 1; y}` that is passed to the
call-by-name parameter `x` of `f` will be evaluated each time `x` is
used in the body of `x`. Thus, this program will print:

```
1
2
```

A simple use case of call-by-name parameters is when you want to
implement functionality for printing debug output in your program
where the printing is conditioned on some Boolean flag that is set
e.g. through a command line parameter:

```scala
def debug(msg: => String): Unit = {
 if (Config.debugOutputEnabled) println(msg)
}
```

If the generation of the debug message `msg` to be printed is
expensive, e.g., because it involves complex diagnostics, then you
want to avoid this overhead in the case where
`Config.debugOutputEnabled` is false. We can achieve this by making
`msg` a call-by-name parameter. In this case the argument expression
passed at the call side is now only executed if
`Config.debugOutputEnabled` is set to true.

A call-by-name parameter can be thought of as a function that is
called with no arguments and then returns a value of type `T`. This
explains the choice for the syntax of call-by-name parameter types in
Scala. This observation can also be used for encoding call-by-name
parameters using call-by-value parameters via anonymous
functions. This is useful in languages that do not support
call-by-name parameters directly. Here is how we can simulate
call-by-name parameters using call-by-value parameters in Scala:

```scala
def f(x: Unit => Int) {
  println(x(()))
  println(x(()))
}

var y = 0

f({_ => y = y + 1; y})
```

Again, this program will print

```
1
2
```

### Custom Control Constructs

We can combine call-by-name parameters with Scala's object system and
its condensed method call syntax. This gives us a powerful technique for
defining custom language primitives that can be used as if they were
built into the language.

For example, some languages such as Pascal support repeat/until loops:
```pascal
repeat body until (cond)
```
These loops execute `body` once, and then repeatedly
execute it until the loop condition `cond` becomes
true. Although, Scala does not have repeat/until loops built in, we
can easily write a class that provides us with such a construct:

```scala
class repeat(body: => Unit) {
  def until(cond: => Boolean): Unit = {
    body
    if (!cond) until(cond) 
  }
}

object repeat {
  def apply(body: => Unit) = new repeat(body)
}
```

The class `repeat` takes the loop body as a parameter and then defines
a method `until` that takes the loop condition to implement a
repeat/until loop using recursion. Since both the loop body and
condition are passed by name, we obtain the expected behavior. The
companion object of `repeat` defines a factory method to
create new `repeat` instances, saving us the explicit calls
to `new`.  

We can now use this class as follows:

```scala
var x = 0
repeat {
  x = x + 1
} until (x == 10)
```

It now seems as if repeat/until is indeed an in-built
language construct. However, this code is just a syntactically more
compact but semantically equivalent version of the following nested
sequence of method calls:

```scala
var x = 0
repeat.apply({
  println(x)
  x = x + 1
}).until(x == 10)
```

In particular, the first call goes to the `apply` method of
the companion object of `repeat`, the subsequent
`until` call then goes to the newly created
`repeat` instance that is returned by the call to
`apply`.
