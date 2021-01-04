/*
* Copyright 2012 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*     http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.eed3si9n.expecty

import language.experimental.macros

abstract class Recorder[A, R] {
  def listener: RecorderListener[A, R]
}

trait UnaryRecorder[A, R] { self : Recorder[A, R] =>
  def apply(recording: A): R = macro RecorderMacro1.apply[A, R]
  def apply(recording: A, message: => String): R = macro RecorderMacro.apply[A, R]
}

trait VarargsRecorder[A, R] { self : Recorder[A, R] =>
  def apply(recordings: A*): R = macro VarargsRecorderMacro.apply[A, R]
}

trait AssertEqualsRecorder[A, R] { self: Recorder [A, R] =>
  def apply(expected: A, found: A): R = macro StringRecorderMacro.apply[A, R]
}
