class Base(val x: Int) {
  val y = 0
  
  println(y)
  
}

class Derived(val z: Int, y1: Int) extends Base(y1) 

object Initialization extends App {

  object A {
    val x: Int = B.x + 1
  }

  object B {
    val x: Int = A.x + 1
  }

  println(s"B: ${B.x}, A: ${A.x}")
  
}
