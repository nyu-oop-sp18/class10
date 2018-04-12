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
