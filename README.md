# Smart Batching Sample

This project is a PoC (Proof-of-Concept) to asynchronous command processing and persisting their outcome to PostgreSQL.
Project has been created from assignment from job interview.

Project is using LMAX disruptor pattern and library, to make asynchronous processing of request with low latency.
Implemented service is mostly using [CQRS](https://www.martinfowler.com/bliki/CQRS.html) pattern.

## Overall design

I like to separate different domains in different packages, instead of more traditional approach, separating different
layers to different packages.
This is in line with [modulith](https://spring.io/projects/spring-modulith) approach and does make easier to separate
public/private API.
Also, this way, it is much easier to see tangling between separate domains.
Splitting project to separate "microservices" can be later done easier.

I am also not fan of using interfaces, when there is no use for them.
They can always be introduced later if needed.

There is no dependency injection used deliberately.

For tests (unit and integration also) we have decided to use [jqwik](https://jqwik.net/) library.
It is an PBT (property based test) library which does build on top of JUnit platform.
It is basicaly
[an alternative test engine for the JUnit 5 platform](https://jqwik.net/docs/current/user-guide.html#how-to-use).

Tests are done in memory with mock-ish implementation of persist service and second one is using postgresql as a
storage.
Testcontainers are taking care of actual postgresql spin up.

### Main packages

There are two main domains in this project. Service (`sk.luvar.service`) and storage (`sk.luvar.database`).
There is no "main app" and showcase of business logic is done solely from tests.

## running

To run tests, there should be sufficient to have working java/maven environment and to have docker installed.
Some integration tests does use testcontainers, whcih does make use of docker.
Some integration tests use solely java in exchange for proper (i.e. postgresql) storage solution.

If you want to use own poistgresql, you can start one in docker:
```shell
# start postgresql in docker
docker run --name smartpg -p 15432:5432 -e POSTGRES_PASSWORD=yourpgpass -d postgres:15.4
# connect to postgresql to wander around using psql
psql --host localhost --port 15432 --password --user postgres
# and type password: "yourpgpass" and hit enter
# than commands "\l+" and "\d+" would show databases and schema for example
```

## Assignment

Create program in Java language that will process commands from FIFO
queue using Producer – Consumer pattern.

Supported commands are the following:

* **Add** - adds a user into a database
* **PrintAll** – prints all users into standard output
* **DeleteAll** – deletes all users from database

**User** is defined as database table **SUSERS** with columns:

* **USER_ID**,
* **USER_GUID** and
* **USER_NAME**

Demonstrate program on the following sequence (using main method
or test):

* Add (1, &quot;a1&quot;, &quot;Robert&quot;)
* Add (2, &quot;a2&quot;, &quot;Martin&quot;)
* PrintAll
* DeleteAll
* PrintAll

Show your ability to unit test code on at least one class.
Goal of this exercise is to show Java language and JDK know-how, OOP
principles, clean code understanding, concurrent programming
knowledge, unit testing experience. Please do not use Spring
framework in this exercise. Embedded database is sufficient.

### Things during implementation

I have post clarification to one stackoverflow question during implementation process.
If curious, have a look at https://stackoverflow.com/a/76987776/3679328