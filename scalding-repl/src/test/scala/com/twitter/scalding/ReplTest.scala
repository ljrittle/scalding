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
package com.twitter.scalding

import org.specs._
import java.util.UUID.randomUUID
import scala.collection.JavaConverters._
import ReplImplicits._

class ReplTest extends Specification {

  val testPath = "/tmp/scalding-repl/test/"
  val helloRef = List("Hello world", "Goodbye world")

  // TODO: replace this convoluted TypedTsv/toIterator/toList once toIterator works on snapshots
  def toIter[T: Manifest: TupleConverter: TupleSetter](snapshot: TypedPipe[T]) = {
    val out = TypedTsv[T](testPath + randomUUID + ".tsv")
    snapshot.save(out)
    out.toIterator
  }

  "A REPL Session" should {

    "save -- TypedPipe[String]" in {
      val hello = TypedPipe.from(TextLine("tutorial/data/hello.txt"))
      val out = TypedTsv[String](testPath + "output0.txt")
      hello.save(out)

      val output = out.toIterator.toList
      output mustEqual helloRef
    }

    "snapshot" in {

      "only -- TypedPipe[String]" in {
        val hello = TypedPipe.from(TextLine("tutorial/data/hello.txt"))
        val s: TypedPipe[String] = hello.snapshot
        // shallow verification that the snapshot was created correctly without
        // actually running a new flow to check the contents (just check that
        // it's a TypedPipe from a SequenceFile)
        s.toString must beMatching("TypedPipe.*SequenceFile")
      }

      "can be mapped and saved -- TypedPipe[String]" in {
        val s = TypedPipe.from(TextLine("tutorial/data/hello.txt"))
          .flatMap(_.split("\\s+"))
          .snapshot

        val out = TypedTsv[String](testPath + "output1.txt")

        // can call 'map' and 'save' on snapshot
        s.map(_.toLowerCase).save(out)

        val output = out.toIterator.toList
        output must_== helloRef.flatMap(_.split("\\s+")).map(_.toLowerCase)
      }

      "tuples -- TypedPipe[(String,Int)]" in {
        val s = TypedPipe.from(TextLine("tutorial/data/hello.txt"))
          .flatMap(_.split("\\s+"))
          .map(w => (w.toLowerCase, w.length))
          .snapshot

        val output = toIter(s).toList
        output must_== helloRef.flatMap(_.split("\\s+")).map(w => (w.toLowerCase, w.length))
      }

      "grouped -- Grouped[String,String]" in {
        val s = TypedPipe.from(TextLine("tutorial/data/hello.txt"))
          .groupBy(_.toLowerCase)
          .snapshot

        // TODO: replace this convoluted TypedTsv/toIterator/toList once toIterator works on snapshots
        val output = toIter(s).toList
        output must_== helloRef.map(l => (l.toLowerCase, l))
      }

      "joined -- CoGrouped[String, Long]" in {
        val linesByWord = TypedPipe.from(TextLine("tutorial/data/hello.txt"))
          .flatMap(_.split("\\s+"))
          .groupBy(_.toLowerCase)
        val wordScores: Grouped[String, Long] =
          TypedPipe.from(OffsetTextLine("tutorial/data/words.txt")).swap.group

        val s = linesByWord.join(wordScores)
          .mapValues{ case (text, score) => score }
          .sum
          .snapshot

        val output = toIter(s).toMap
        output must_== Map("hello" -> 0, "goodbye" -> 2, "world" -> 2)
      }
    }

    "reset flow" in {
      resetFlowDef()
      flowDef.getSources.asScala must beEmpty
    }

    "run entire flow" in {
      resetFlowDef()
      val hello = TypedPipe.from(TextLine("tutorial/data/hello.txt"))
        .flatMap(_.split("\\s+"))
        .map(_.toLowerCase)
        .distinct

      val out = TypedTsv[String](testPath + "words.tsv")

      hello.write(out)
      run

      val words = out.toIterator.toSet
      words must_== Set("hello", "world", "goodbye")
    }
  }

}
