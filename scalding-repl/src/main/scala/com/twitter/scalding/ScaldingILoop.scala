/*  Copyright 2013 Twitter, inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.twitter.scalding

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.ILoop

/**
 * A class providing Scalding specific commands for inclusion in the Scalding REPL.
 */
class ScaldingILoop
  extends ILoop {
  override def printWelcome() {
    echo(" (                                           \n" +
      " )\\ )            (   (                       \n" +
      "(()/(         )  )\\  )\\ )  (          (  (   \n" +
      " /(_)) (   ( /( ((_)(()/( )\\   (     )\\))(  \n" +
      "(_))   )\\  )(_)) _   ((_)((_)  )\\ ) ((_))\\  \n" +
      "/ __| ((_)((_)_ | |  _| | (_) _(_/(  (()(_) \n" +
      "\\__ \\/ _| / _` || |/ _` | | || ' \\))/ _` |  \n" +
      "|___/\\__| \\__,_||_|\\__,_| |_||_||_| \\__, |  \n" +
      "                                    |___/   ")
  }

  /**
   * Commands specific to the Scalding REPL. To define a new command use one of the following
   * factory methods:
   * - `LoopCommand.nullary` for commands that take no arguments
   * - `LoopCommand.cmd` for commands that take one string argument
   * - `LoopCommand.varargs` for commands that take multiple string arguments
   */
  private val scaldingCommands: List[LoopCommand] = List()

  /**
   * Change the shell prompt to read scalding&gt;
   *
   * @return a prompt string to use for this REPL.
   */
  override def prompt: String = "\nscalding> "

  /**
   * Gets the list of commands that this REPL supports.
   *
   * @return a list of the command supported by this REPL.
   */
  override def commands: List[LoopCommand] = super.commands ++ scaldingCommands

  addThunk {
    intp.beQuietDuring {
      intp.addImports("com.twitter.scalding._", "com.twitter.scalding.ReplImplicits._")
    }
  }
}
