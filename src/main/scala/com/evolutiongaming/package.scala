package com

package object evolutiongaming {

  def tap[A, B](obj: A)(f: A => B): A = { f(obj); obj }

  // when ... extends AnyVal then class becomes a value class
  // and thus all calls converted into direct static calls
  // without instantiating the class
  class TapAny[A](val obj: A) extends AnyVal {
    def tap[B](f: A => B): A = { f(obj); obj }
  }

  implicit def tapAny[T](obj: T): TapAny[T] = new TapAny[T](obj)

}
