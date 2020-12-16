#Lesson Learnt

## RabbitMQ

### Routing Key, Binding, Exchange
Message is not send directly to queue. It is send to exchange with a routing key. 
A queue is bind to exchange with routing key.

A message is send to the exchange with routing key,
When the exchange process the message, then the exchange route to the queue by the routing key.

[ ] How does it work for Spring Cloud Stream?