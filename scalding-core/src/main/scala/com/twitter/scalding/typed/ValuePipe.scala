/*
Copyright 2012 Twitter, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.twitter.scalding.typed

import com.twitter.algebird._
import com.twitter.scalding.{ Mode, IterableSource }

import com.twitter.scalding.Execution

object ValuePipe extends java.io.Serializable {
  implicit def toTypedPipe[V](v: ValuePipe[V]): TypedPipe[V] = v.toTypedPipe

  def fold[T, U, V](l: ValuePipe[T], r: ValuePipe[U])(f: (T, U) => V): ValuePipe[V] =
    l.leftCross(r).collect { case (t, Some(u)) => f(t, u) }

  def apply[T](t: T): ValuePipe[T] = LiteralValue(t)
  def empty: ValuePipe[Nothing] = EmptyValue
}

/**
 * ValuePipe is special case of a TypedPipe of just a optional single element.
 *  It is like a distribute Option type
 * It allows to perform scalar based operations on pipes like normalization.
 */
sealed trait ValuePipe[+T] extends java.io.Serializable {
  def leftCross[U](that: ValuePipe[U]): ValuePipe[(T, Option[U])] = that match {
    case EmptyValue => map((_, None))
    case LiteralValue(v2) => map((_, Some(v2)))
    // We don't know if a computed value is empty or not. We need to run the MR job:
    case _ => ComputedValue(toTypedPipe.leftCross(that))
  }
  def collect[U](fn: PartialFunction[T, U]): ValuePipe[U] =
    filter(fn.isDefinedAt(_)).map(fn(_))

  def map[U](fn: T => U): ValuePipe[U]
  def filter(fn: T => Boolean): ValuePipe[T]
  def toTypedPipe: TypedPipe[T]

  def toOptionExecution: Execution[Option[T]] =
    toTypedPipe.toIteratorExecution.map { it =>
      it.take(2).toList match {
        case Nil => None
        case h :: Nil => Some(h)
        case items => sys.error("More than 1 item in an ValuePipe: " + items.toString)
      }
    }

  def debug: ValuePipe[T]
}
case object EmptyValue extends ValuePipe[Nothing] {
  override def leftCross[U](that: ValuePipe[U]) = this
  override def map[U](fn: Nothing => U): ValuePipe[U] = this
  override def filter(fn: Nothing => Boolean) = this
  override def toTypedPipe: TypedPipe[Nothing] = TypedPipe.empty

  def debug: ValuePipe[Nothing] = {
    println("EmptyValue")
    this
  }
}
case class LiteralValue[T](value: T) extends ValuePipe[T] {
  override def map[U](fn: T => U) = LiteralValue(fn(value))
  override def filter(fn: T => Boolean) = if (fn(value)) this else EmptyValue
  override def toTypedPipe = TypedPipe.from(Iterable(value))

  def debug: ValuePipe[T] = map { v =>
    println("LiteralValue(" + v.toString + ")")
    v
  }
}
case class ComputedValue[T](override val toTypedPipe: TypedPipe[T]) extends ValuePipe[T] {
  override def map[U](fn: T => U) = ComputedValue(toTypedPipe.map(fn))
  override def filter(fn: T => Boolean) = ComputedValue(toTypedPipe.filter(fn))

  def debug: ValuePipe[T] = map { value =>
    println("ComputedValue(" + value.toString + ")")
    value
  }
}
