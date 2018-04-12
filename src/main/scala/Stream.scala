trait Stream[+A] {
  import Stream._
  
  def isEmpty: Boolean
  def head: A
  def tail: Stream[A]
}

object Stream {
  def cons[A](hd: A, tl: => Stream[A]): Stream[A] = new Stream[A] {
    override def isEmpty = false
    override def head = hd
    override def tail = tl
  }
  
  def empty: Stream[Nothing] = new Stream[Nothing] {
    override def isEmpty = true
    override def head = throw new NoSuchElementException("empty.head")
    override def tail = throw new NoSuchElementException("empty.tail")
  }
}









object Test extends App {
  import Stream._
  
  def nat: Stream[Int] = {
    def loop(x: Int): Stream[Int] = cons(x, loop(x + 1))
    loop(0)
  }
  
  def isPrime(n: Int): Boolean = (2 to math.sqrt(n).toInt) forall (x => n % x != 0)
  
}