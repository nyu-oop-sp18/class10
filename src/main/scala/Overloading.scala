import scala.language.implicitConversions

object Overloading extends App {

  class A {
    def m(x: A): A = { println("A.m(A)"); new B() }
    def m(x: A, y: A): Unit = { println("A.m(A, A)") }
    def m(x: A, y: B): Unit = { println("A.m(A, B)") }
    def m(x: B, y: A): Unit = { println("A.m(B, A)") }
    def m(x: C, y: C): Unit = { println("A.m(C, C)") }
    def m(x: Any): Unit = { println("A.m(Any)") }
    def m(x: Short): Unit = { println("A.m(Short)") }
    def m(a: Int): Unit = { println("A.m(Int)") }
    def m(x: Long): Unit = { println("A.m(Long)") }
    def m(x: Double): Unit = { println("A.m(Double)") }
  }
  
  class B extends A {
    override def m(x: A): A = { println("B.m(A)"); new A() }
    def m(x: B): Unit = { println("B.m(B)") }
  }
  
  class C extends B
  
  val a = new A()
  val b = new B()
  val c = new C()
  val s1: Short = 1
  val s2: Short = 1

  b.m(a)
  b.m(b)
  b.m(c)
  a.m(a, a)
  a.m(c, c)
  
  implicit def fromString(x: String): A = new A()
  
}