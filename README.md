# Scala Pet Store
An implementation of the java pet store using FP techniques in scala.

## Why you doing dis?
The goal for this project is to demonstrate how to build an application using FP techniques in Scala.
When starting out in Scala coming from a Java background, it was extremely difficult to piece together all of the little
bits in order to make a cohesive whole application.

## How are you build it?
As the goal of the project is to help Java / Spring folks understand how to build an application in Scala, I will
not be looking to employ the more esoteric features in Scala like Type Classes and Category Theory.  Those things will
be present, but I hope to obscure them for the purpose of allowing the reader to understand what is going on in the code.

I will reach out to Java developers along the way, to see if techniques that I use are too confusing, or have a low
enough barrier to entry to pick up quickly.

## What is your stack?
I am going to work with the TypeLevel stack initially and see how far I can go before the code becomes too obtuse.
The stack I am looking at follows:

- HTTP4S as the web server.  I could have gone with finch, twitter server, or akka-http here as well, but I have been
interested in learning http4s, so the decision is rather self motivating.
- Circe for json serialization.
- Tagless Final for my core domain.
- DB access is unknown, I will try doobie, but may fall back to scalikejdbc if I cannot have it align with the goals for the project

## Getting Started


