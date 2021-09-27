### Build and run
The repository provides `docker-compose.yml` file for running Postgres DB.
Execute the following:
`docker-compose up -d` to start DB in background
`sbt core/run` to start application
There are two possible implementations based on Cats IO (IOMain file) and ZIO Task (ZioMain file), so you can choose
what implementation to use in sbt startup menu. Or just to comment out one implementation and skip what to choose
on startup.

When you're done, ^C to kill the Scala server and
`docker-compose down`

### Implementation considerations
There is a requirement in task description that headlines must be stored in DB. I decided to add a background job,
which stores fetched headlines in DB with some period in time. In turn graphQL endpoint fetches data from DB.
Cron job configuration is in config file, so it's could be easily adjusted how often headlines should be fetched.
If new headlines were fetched then old data is removed and new is inserted. I made this simple implementation, but 
could be changed depending on business needs, e.g. if we have to fetch often it's better to append new records
and fetch those new records.
Also, this approach gives such pros:
+ Good performance. Service returns data from database and doesn't depend on performance of crawler service.
+ No issues with concurrency and synchronisation. There is no need to sync data between http4s incoming requests
and crawler service.
+ The logic how to store and fetch data could be easily changed. E.g. we can store everything and fetch based on data 
or version number or something else.

Cons:
+ Because we remove all data and then insert new entries there could be degradation in performance at this moment(
locking DB)

### Accessing API
After service is started it can be queried with graphQL request
```shell
curl --location --request POST 'http://localhost:8080/graphql' \
 --header 'Content-Type: application/json' \
 --data-raw '{"query":"query {\n    news {\n        link\n        title\n    }\n}"}'
```

### Possible improvements
- More tests (some libraries is hard to mock and test), integration tests
- Better logging, metrics



### Instructions

This test will be used to evaluate your skills as a functional programmer, as well as code organization and cleanliness and test coverage.

Using functional programming techniques in Scala, please develop a backend application exposing the following GraphQL schema:

```
type News {
  title: String!,
  link: String!,
}

type Query {
  news: [News!]!
}
```

Then, create a crawler that will scrape all news headlines from nytimes.com and expose them using the GraphQL API.

Also, add a persistence layer to store all headlines collected using the following schema:

```
CREATE TABLE headlines (
  link VARCHAR PRIMARY KEY,
  title VARCHAR NOT NULL
);
```

#### Restrictions:

Use the following libraries:

- Sangria
- Http4s
- Sttp
- Cats/Cats-Effect
- Monix/Zio
- Scala-scrapper
- Quill

**Nice to have:**

Abstract the effect and instantiate the program using two IO/Task implementations (Cats-Effect, Monix, Zio);
functional state management.

_This test will be used to evaluate your skills as a functional programmer, as well as code organization and cleanliness and test coverage._


___

The average time a programmer takes to complete this assignment is 6 hours.

We know you're probably busy for the most part of the day so you can take a week to deliver the final version.

