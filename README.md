# Scala Pet Store
An implementation of the java pet store using FP techniques in scala.

# Status
** Currently, this is under active development until the relevant bits fall out.  Feel free to contribute thoughts, ideas
   or code.  Will socialize more widely once enough folks have reviewed and more parts are built out **

This is very early on, there are lots of bits that I want to fall in here:

- Scalacheck + Scalatest for unit testing
- ??? for integration testing.  Figure could use H2
- ??? some kind of user interface, we have been working with react, so that is where we might wind up
- Authentication


## Why you doing this?
The goal for this project is to demonstrate how to build an application using FP techniques in Scala.
When starting out in Scala coming from a Java background, it was extremely difficult to piece together all of the little
bits in order to make a cohesive whole application.

## How are you build it?
As the goal of the project is to help Java / Spring folks understand how to build an application in Scala, I will
not be looking to employ the more esoteric features in Scala like Type Classes and Category Theory.  Those things will
be present, but I hope to obscure them in parts the purpose of allowing the reader to understand what is going on in the code.

I will reach out to Java developers along the way, to see if techniques that I use are too confusing, or have a low
enough barrier to entry to pick up quickly.

## What is your stack?
I am going to work with the TypeLevel stack initially and see how far I can go with it.  I believe that framing the
concepts in code in an easy to understand way should be possible with Typelevel.

- HTTP4S as the web server.  I could have gone with finch, twitter server, or akka-http here as well, but I have been
interested in learning http4s.
- Circe for json serialization.
- Tagless Final for my core domain.
- DB access is unknown, I will try doobie, but may fall back to scalikejdbc if I cannot have it align with the goals for the project

## Getting Started

Start up sbt:

```
> sbt
```

Once sbt has loaded, you can start up the application

```
> ~re-start
```

This uses revolver, which is a great way to develop and test the application.  Doing things this way the application
will be automatically rebuilt when you make code changes

To stop the app in sbt, hit the `Enter` key and then type:

```
> re-stop
```

## Testing
Still need to do some more comprehensive testing.  For the time being, you can test as follows:

First, make sure you start up the app using the directions above.

Then, create a pet in a separate terminal

```
> ./post-pet.sh
...
{"name":"Harry","typ":"Cat","bio":"I am fuzzy","id":5297756703253440921}
```

You will see the output, you will have to remember the id.

Next, you can `GET` the pet (replace your id with whatever id was generated above)...

```
> curl http://localhost:8080/pet?id=5297756703253440921
```

Finally, you can delete the bet, just have to use the delete verb.  Again, use your id...

```
> curl -X DELETE http://localhost:8080/pet?id=5297756703253440921
```

You can verify the pet has been deleted by doing a get again.  You will see a NotFound response.


