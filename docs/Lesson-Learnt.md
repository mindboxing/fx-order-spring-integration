#Lesson Learnt

## RabbitMQ

### Routing Key, Binding, Exchange
Message is not send directly to queue. It is send to exchange with a routing key. 
A queue is bind to exchange with routing key.

A message is send to the exchange with routing key,
When the exchange process the message, then the exchange route to the queue by the routing key.

[ ] How does it work for Spring Cloud Stream?

## Message Header ID
The value change at every exchange.
For example, a message consume from RaabitMQ the message ID is X.
After consume and flow to the next chain (handle, transformer etc), the message ID changes.

## Message Store as Message Channel
Mongo DB can be setup to consume the message and persist in DB.
The message will be poll and release to downstream process by polling.
This ensures message is not lost qnd is throttle to downstream application to process.
The downside is increase in latency.

## Error Channel
The SI will setup errorChannel. It is design for asynchronous operation.
In an asynchronous operation exception could not propagate to the caller with respect to the thread.
Hence, exception is a message and is handle by passing to errorChannel. 
When the message is send through direct channel, then it behaves like a synchronous operation.
The MessageHandlingException will be propagating like regular java call stack.

## Unit Test for SI
The unit test serve as quality gate. It can be run in CI environment.
The dependency backing services such as mongo, rabbitmq etc are unavailable in CI environment.
The options are using mocks, embedded or testcontainer.
The testcontainer will be challenging for mongo container.
The image require database and user setup. 
The embedded and mock may solve the test problem, but it
also force the code to be develop to be test friendly. 