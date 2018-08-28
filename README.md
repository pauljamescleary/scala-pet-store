Scala Pet Store [![Build Status](https://travis-ci.org/pauljamescleary/scala-pet-store.svg?branch=master)](https://travis-ci.org/pauljamescleary/scala-pet-store)
[![Chat](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/scala-pet-store/scala-pet-store)
==============
An implementation of the java pet store using FP techniques in scala.

# Thank you!
Special thank you to the many [contributors](AUTHORS.md) who continue to improve the pet store

# Status
I have most of the endpoints in place.  There are few big pieces remaining in case y'all want to lend a hand:

1. Use TSec to add login / authentication using JWT
2. Create a ScalaJS React front end
3. Build tests using scala check in an idiomatic sane way
4. Create a microsite documenting the patterns in the pet store.  Kind of a mini-book

# Want to help out?
[Join the chat at Scala Pet Store gitter.im](https://gitter.im/scala-pet-store/scala-pet-store)

If you have general feedback on how things could be better, feel free to post an issue / gist or
open a PR!  I am looking for ideas like:

* If you are a FP or TypeLevel contributor, let me know if something is not idiomatic as per FP or one
of the TypeLevel libs I am using.  I want this app to be an example of how things _should_ be done as
much as possible.
* If you are an OO dev new-ish to Scala, let me know if something is confusing.  I am trying to keep this
project approachable.  Feel free to email me or open an issue.

## Why you doing this?
The goal for this project is to demonstrate how to build an application using FP techniques in Scala.
When starting out in Scala coming from a Java background, it was extremely difficult to piece together all of the little
bits in order to make a cohesive whole application.

## How are you building it?
As the goal of the project is to help Java / Spring folks understand how to build an application in Scala.  I hope
to use good practice / conventions using FP and Scala, while maintaining approachability for OO peeps.

I will reach out to Java developers along the way, to see if techniques that I use are too confusing, or have a low
enough barrier to entry to pick up quickly.

## What is your stack?
I am going to work with the TypeLevel stack initially and see how far I can go with it.  I believe that framing the
concepts in code in an easy to understand way should be possible with Typelevel.

- [Http4s](http://http4s.org/) as the web server.  I could have gone with finch, twitter server, or akka-http here as well, but I have been
interested in learning http4s
- [Circe](https://circe.github.io/circe/) for json serialization
- [Doobie](https://github.com/tpolecat/doobie) for database access
- [Cats](https://typelevel.org/cats/) for FP awesomeness
- [ScalaCheck](https://www.scalacheck.org/) for property based testing
- [PureConfig](https://pureconfig.github.io/docs/) for app config
- Tagless Final for the core domain.

## Getting Started

Start up sbt:

```
> sbt
```

Once sbt has loaded, you can start up the application

```
> ~reStart
```

This uses revolver, which is a great way to develop and test the application.  Doing things this way the application
will be automatically rebuilt when you make code changes

To stop the app in sbt, hit the `Enter` key and then type:

```
> reStop
```

## Testing
Building out a test suite using Python.  The reason is that typically we want to run tests against a live environment
when we deploy our code in order to make sure that everything is running properly in the target environment.  It
is reassuring to know that your code works across clients.

In order to run the functional tests, your machine will need to have Python 2.7 and pip, and virtualenv.

1. To install pip on a Mac, run `sudo easy_install pip`
2. Then install virutalenv `sudo pip install virtualenv`

To test out the app, first start it up following the directions above and doing `reStart`

Then, in a separate terminal, run the test suite:

```
> cd functional_test
> ./run.py live_tests -v
```



