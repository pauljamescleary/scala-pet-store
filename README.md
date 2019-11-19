Scala Pet Store [![Build Status][build-badge]][build-link] [![Chat][gitter-badge]][gitter-link]
==============
An implementation of the java pet store using FP techniques in scala.

# Thank you!
Special thank you to [Zak Patterson](https://github.com/zakpatterson) who also maintains this project; as well as the many [contributors](AUTHORS.md) who continue to improve the pet store

# Status
I have most of the endpoints in place.  There are few big pieces remaining in case y'all want to lend a hand:

1. Create a ScalaJS React front end
2. Build tests using scala check in an idiomatic sane way
3. Create a microsite documenting the patterns in the pet store.  Kind of a mini-book

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
- [Circe Config](https://github.com/circe/circe-config) for app config
- Tagless Final for the core domain.

## What is going on here!?
This project has developed over-time and has embraced some traditional OO concepts, in addition to modern FP concepts and libraries.  Let's talk about the foundational design patterns that emerge in the pet store.

### Domain Driven Design (DDD)
Domain driven design is all about developing a _ubiquitous language_, which is a language that you can use to discuss your software with business folks (who presumably do not know programming).  The key concept is that language surfaces in your code.  Gotta thing called a "Pet", I should see a `Pet` in my code.  I strongly recommend the book [Domain Driven Design by Eric Evans](https://www.amazon.com/exec/obidos/ASIN/0321125215/domainlanguag-20).

The book discusses a lot of patterns, some of those we see in play in the pet store.  DDD is all about making your code expressive, making sure that how you _talk_ about your software materializes in your code.  One of the best ways to do this is to keep you _domain_ pure.  That is, allow the business concepts and entities to be real things, and keep all the other cruft out.  For example, while it is valuable to know that a `Transaction` in banking relies on a `Debit` as well as a `Credit`; these concepts should surface in your domain someplace.  However, HTTP, JDBC, SQL are not essential to your domain, so you want to _decouple_ those as much as possible.

### Onion (or Hexagonal) Architecture
In concert with DDD, the [Onion Architecture](https://jeffreypalermo.com/2008/07/the-onion-architecture-part-1/) and [Hexagonal Architecture from Cockburn](https://java-design-patterns.com/patterns/hexagonal/) give us patterns on how to separate our domain from the ugliness of implementation.

We fit DDD an Onion together via the following mechanisms:

**The domain package**
The domain package constitutes the things inside our domain.  It is deliberately free of the ugliness of JDBC, JSON, HTTP, and the rest.  We use `Services` as coarse-grained interfaces to our domain.  These typically represent real-world use cases.  We see a lot of CRUD in the pet store, but use cases can be things like _withdrawl_, or _register_ in other domains.  Often times, you see a 1-to-1 mapping of `Services` to `Endpoints` or HTTP API calls your application surfaces.

Inside of the **domain**, we see a few concepts:

1. `Service` - the coarse grained use cases that work with other domain concepts to realize your use-cases
1. `Repository` - ways to get data into and out of persistent storage.  **Important: Repositories do not have any business logic in them, they should not know about the context in which they are used, and should not leak details of their implementations into the world**.
1. `models` - things like `Pet`, `Order`, and `User` are all domain objects.  We keep these lean (i.e. free of behavior).  All of the behavior comes via `Validations` and `Services`

Note that `Repository` is kind of like an _interface_ in Java.  It is a `trait` that is to be implemented elsewhere.

**The infrastructure package**
The infrastructure package is where the ugliness lives.  It has HTTP things, JDBC things, and the like.

1. `endpoint` - contains the HTTP endpoints that we surface via **http4s**.  You will also typically see JSON things in here via **circe**
1. `repository` - contains the JDBC code, implementations of our `Repositories`.  We have 2 implementations, an in-memory version as well as a **doobie** version.

**The config package**
The config package could be considered infrastructure, as it has nothing to do with the domain.  We use **Circe Config** to load configuration objects when the application starts up.  **circe config** Provides a neat mapping of config file to case classes for us, so we really do not have to do any code. 

### What about dependency injection?
The pet store does currently use `classes` for certain things (some would argue this isn't very FP).  There are lots of ways to do dependency injection, including function arguments, implicits, and monad transformers.  Using _class constructors_ is rather OO like, but I believe this is simpler for people with OO backgrounds to digest.

There is no spring, guice, or other dependency injection / inversion of control (IoC) framework at use here.  The author of the pet store is strongly opinionated against these kinds of libraries.  

### Fitting it all together
The idea with FP in general is to keep your domain pure, and to push the ugliness to the edges (which we achieve in part via DDD and Hexagonal Architecture).  The way the application is bootstrapped is via the `Server` class.  It's job is to make sure that all the parts are configured and available so that our application can actually start up.  The `Server` will

1. Load the configuration using pure config.  If the user has not properly configured the app, it will not start
1. Connect to the database.  Here, we also run **flyway** migrations to make sure that the database is in good order.  If the database cannot be connected to, the app will not start
1. Create our `Repositories` and `Services`.  This wires together our domain.  We do not use any kind of dependency injection framework, rather we pass instances where needed using **constructors**
1. Bind to our port and expose our services.  If the port is unavailable, the app will not start

### What is with this F thing?
You see in most of the core domain that we use `F[_]` in a lot of places.  This is called a _higher kinded type_, and simply represents a type that holds (or works with) another type.  For example, `List` and `Option` are examples of types that hold other types, like `List[Int]` or `Option[String]`.

We use `F[_]` to mean "some effect type".  We can leave this abstract, and bind to it "at the end of the world" in the `Server` when we bootstrap the application.  This demonstrates the idea of late binding, leave your code abstract and only bind to it when absolutely necessary.

When you see a signature like `def update(pet: Pet)(implicit M: Monad[F]):`, we are saying that the `F[_]` thing must have a `Monad` type class available at the call site.

In this application, we use **cats effect IO** as our effect type, and use **cats** for _Monads_ and other FP type classes and data types.  We could just as easily use **scalazIO** and **scalaz** in an alternative implementation without changing the code dramatically.

## Getting Started

Be aware that this project targets **Java 11**.

Start up sbt:

```bash
sbt --java-home {your.java.11.location}
```

Once sbt has loaded, you can start up the application

```sbtshell
> ~reStart
```

This uses revolver, which is a great way to develop and test the application.  Doing things this way the application
will be automatically rebuilt when you make code changes

To stop the app in sbt, hit the `Enter` key and then type:

```sbtshell
> reStop
```

## Testing
Building out a test suite using Python.  The reason is that typically we want to run tests against a live environment
when we deploy our code in order to make sure that everything is running properly in the target environment.  It
is reassuring to know that your code works across clients.

In order to run the functional tests, your machine will need to have Python 3 and pip, and virtualenv.

1. To install pip on a Mac, run `sudo easy_install pip`
2. Then install virutalenv `sudo pip install virtualenv`

To test out the app, first start it up following the directions above and doing `reStart`

Then, in a separate terminal, run the test suite:

```
> cd functional_test
> ./run.py live_tests -v
```


[build-badge]: https://travis-ci.com/pauljamescleary/scala-pet-store.svg?branch=master
[build-link]: https://travis-ci.com/pauljamescleary/scala-pet-store
[gitter-badge]: https://badges.gitter.im/Join%20Chat.svg
[gitter-link]: https://gitter.im/scala-pet-store/scala-pet-store
