# Expecty - Power Assertions for Scala

Expecty brings power assertions as known from [Groovy](http://groovy.codehaus.org) and [Spock](http://spockframework.org)
to the [Scala](http://scala-lang.org) language. It is a micro library that aims to do one thing well.

## License and Credits

Expecty was originally written by Peter Niederwieser, author of Spock.

Expecty is licensed under the Apache 2 license.

## Setup

```scala
libraryDependencies += "com.eed3si9n.expecty" %% "expecty" % "0.11.0" % Test
```

## Code Examples

```scala
import org.expecty.Expecty.assert

case class Person(name: String = "Fred", age: Int = 42) {
  def say(words: String*) = words.mkString(" ")
}

val person = Person()

// Passing expectations

assert {
  person.name == "Fred"
  person.age * 2 == 84
  person.say("Hi", "from", "Expecty!") == "Hi from Expecty!"
}

// Failing expectation

val word1 = "ping"
val word2 = "pong"

assert {
  person.say(word1, word2) == "pong pong"
}

/*
Output:

java.lang.AssertionError:

person.say(word1, word2) == "pong pong"
|      |   |      |      |
|      |   ping   pong   false
|      ping pong
Person(Fred,42)
*/

// Continue despite failing predicate

val expect2 = new Expecty(failEarly = false)

expect2 {
  person.name == "Frog"
  person.age * 2 == 73
}

/*
Output:

java.lang.AssertionError:

person.name == "Frog"
|      |    |
|      Fred false
Person(Fred,42)


person.age * 2 == 73
|      |   |   |
|      42  84  false
Person(Fred,42)
*/
```

## Further Examples

Have a look at [ExpectySpec.scala](https://github.com/pniederw/expecty/blob/master/src/test/scala/org/expecty/ExpectySpec.scala)
and other specs in the same directory.


 
