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

import org.specs._

import com.twitter.scalding._
import com.twitter.algebird.monad.Reader

// Need this to flatMap Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.util.Try

// this is the scalding ExecutionContext
import ExecutionContext._

object ExecutionTestJobs {
  def wordCount(in: String, out: String) = { implicit ctx: ExecutionContext =>
    TypedPipe.from(TextLine(in))
      .flatMap(_.split("\\s+"))
      .map((_, 1L))
      .sumByKey
      .write(TypedTsv(out))
  }
}

class WordCountEc(args: Args) extends ExecutionContextJob[Any](args) {
  def job = ExecutionTestJobs.wordCount(args("input"), args("output"))
}

class ExecutionTest extends Specification {
  "An Executor" should {
    "work synchonously" in {
      val (r, stats) = Execution.waitFor(Config.default, Local(false)) { implicit ec: ExecutionContext =>
        val sink = new MemorySink[(Int, Int)]
        TypedPipe.from(0 to 100)
          .map { k => (k % 3, k) }
          .sumByKey
          .write(sink)

        { () => sink.readResults }
      }
      stats.isSuccess must beTrue
      r().toMap must be_==((0 to 100).groupBy(_ % 3).mapValues(_.sum).toMap)
    }
    "work asynchonously" in {
      val (r, fstats) = Execution.run(Config.default, Local(false)) { implicit ec: ExecutionContext =>
        val sink = new MemorySink[(Int, Int)]
        TypedPipe.from(0 to 100)
          .map { k => (k % 3, k) }
          .sumByKey
          .write(sink)

        { () => sink.readResults }
      }
      Try(Await.result(fstats, Duration.Inf)).isSuccess must beTrue
      r().toMap must be_==((0 to 100).groupBy(_ % 3).mapValues(_.sum).toMap)
    }
  }
  "A ExecutionJob" should {
    "run correctly" in {
      JobTest("com.twitter.scalding.typed.WordCountEc")
        .arg("input", "in")
        .arg("output", "out")
        .source(TextLine("in"), List((0, "hello world"), (1, "goodbye world")))
        .sink[(String, Long)](TypedTsv[(String, Long)]("out")) { outBuf =>
          val results = outBuf.toMap
          results must be_==(Map("hello" -> 1L, "world" -> 2L, "goodbye" -> 1L))
        }
        .run
        .runHadoop
        .finish
    }
  }
}
