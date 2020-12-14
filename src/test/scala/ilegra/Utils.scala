package ilegra

import java.time.Instant

import org.scalacheck.{Arbitrary, Gen, ScalacheckShapeless}
import org.scalacheck.rng.Seed

import scala.util.{Success, Try}

object Utils {
  def anyRandomOf[T: Arbitrary](): T = anyRandomOf[T](1).head

  def anyRandomOf[T: Arbitrary](number: Int = 1): Stream[T] = {
    val gen = Gen.infiniteStream(implicitly[Arbitrary[T]].arbitrary)
    Try(gen.apply(Gen.Parameters.default, Seed(Instant.now().toEpochMilli))) match {
      case Success(Some(v)) => v.take(number)
      case _                => throw new RuntimeException("cannot generated random data from given type")
    }
  }
}
