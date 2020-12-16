# FX Position Dataflow [FX-P-DF]
This process FX positions.
This project is for training the usage of spring integration.

## Setup MongoDB
This project comes with docker-compose.
MongoDB will require setup of database and user.
Please look at (mongodb.md)[./docs/mongodb.md] for setting it up

## Java 1.5
Yeah, I'm exploring the unknown.
If you are looking into this, please do so.
You will need to install java 1.5.

## For a Spin
When docker-compose is up and database is setup.
This spring boot application should be good to go.
There is also a unit test MessageFlowConfigTest.java

## Responsibility
### Consume 
[DONE] The message is receive as XML message from fx.in.
[ ] UT: The message is receive as XML message from fx.in.
[DONE] the message will be persist    
[ ] UT: the message will be persist
 [] Idempotent Receiver

### Convert
[DONE] It will be map to normalized data.
 [] UT

### Process
[DONE] It will validate that the RT position sufficiently funded.
[] UT
[DONE] It will enrich with fund flag as true or false.
[] UT

### Route
[DONE] When fund flag is false, create ORDER_REJECTED and publish to fx.event
[] UT
[DONE] When fund flag is false, create ORDER_ACCEPTED and publish to fx.event
[] UT

### Record
[] It persists consumed XML.
[] It persists event. 
[] It persists request sent and response received for interaction with other services. 

### Publish
[DONE] On pass validation, it publishes ORDER_ACCEPTED event.
[DONE] On fail validation, it publishes ORDER_REJECTED event.

### Error Handling
[] Connection issue with fund-service
    [DONE] Retryable : SocketTimeoutException
[] Fund-service - 400
    [] Log
[] Fund-service - 500
    [] Log
[] Bad XML
    [] Log
[] Bad Data
    [] Log

## Interaction

```
-(fx.position)->  [Fx-Position-Dataflow]

[Fx-Position-Dataflow] -(fx.event)->

[Fx-Position-Dataflow] <-REST-> [Collateral Service]

[Fx-Position-Dataflow] <-TCP-> ( Mongo )
```

### Inbound
#### Rabbit MQ
- fx.position

### Outbound
#### Rabbit MQ
- fx.event

### Service Consume
#### Collateral Service
POST http://collateral-service/account/{accountId}/fundingCheck
    It is use for checking that the trading account has sufficient fund for the FX position.

### Persistence
Mongo

It persists the following
- request to fund-service
- response to fund-service
- event generated
- request receive    

## Non-functional
### Idempotent
Messaging system does not guarantee deliver exactly once.
It is possible that message is redeliver.

### Runtime Error
Any runtime error will be send to error queue.
Runtime error are non-business error. 
It will be track for troubleshooting and resolution.
[ ] error handling: runtime

### Replay
Any message process failed, can be replay and continue from where it fail. 
It will be replay by receiving the same message and re-process it.

### Business Exception
Business Exception will be handle as an event and publish.
[ ] error handling: business

### Integration Test
This project is using mockserver as the fund-service

### Contract Test
Contract-test with collateral-service

### Input Message Version Changes
Since the xsd is agreed. The change should be in sync.
If the old message is process, then it should be consume and process as per the new version.
This is achieve by adding a new Integration Flow

## Glossary
| Abbreviation | Description |
| --- | --- |
| RT | Retail Trader. This is a person who has a trading account |


## Reference
- [spring-amqp](https://docs.spring.io/spring-amqp/reference/html/)
- [amqp](https://docs.spring.io/spring-integration/reference/html/amqp.html)
- [messaging-rabbitmq](https://spring.io/guides/gs/messaging-rabbitmq/)
- [Spring Integration](https://docs.spring.io/spring-boot/docs/2.4.0/reference/htmlsingle/#boot-features-integration)
- [Spring for RabbitMQ](https://docs.spring.io/spring-boot/docs/2.4.0/reference/htmlsingle/#boot-features-amqp)
- [Error Handling](https://docs.spring.io/spring-integration/reference/html/error-handling.html#:~:text=Spring%20Integration%20supports%20error%20handling,to%20the%20'replyChannel'%20resolution.)
- [Mock Server](https://www.mock-server.com/mock_server/running_mock_server.html)
- [MongoDB SI Support](https://docs.spring.io/spring-integration/reference/html/mongodb.html#mongodb-connection)
- [Idempotent Receiver](https://docs.spring.io/spring-integration/docs/current/reference/html/messaging-endpoints.html#idempotent-receiver)



