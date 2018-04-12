class repeat[A](body: => A) {
  def until(cond: => Boolean): A = {
    lazy val res = body
    if (!cond) until(cond) else res
  }
}

object repeat {
  def apply[A](body: => A): repeat[A] = new repeat(body)
}

object CallByName extends App {
  
  var x = 1
  
  repeat {
    println(x)
    x += 1
  } until (x > 10)
  
}
