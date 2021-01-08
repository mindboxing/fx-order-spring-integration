# Error Handling

Any exception thrown should be routed to error channel.
It will be log and sent to  dead letter queue.

## Reprocess Message
Message on receive will be saving to database.
The payload can be lookup by messageId.
The reprocess command will resend the payload and process from the beginning.

## Recoverable Exception

As of now the following are the known recoverable error.

**java.net.SocketTimeoutException**

This is due to unexpected longer  REST service response time. (fundSufficientCheck etc)
It could be overloaded.
The strategy is to retry 2 times with an interval of 2 seconds.
After 2 retry, it will be sent to dead letter queue.

The header message messageId will be use for replaying.
The orderId is use for a payload retrieval.

## Intervention Require

**Status code 400 or 500**

Although a valid message sent successfully, but was response with status code 400.
This required a review of the error. It could be due to bug or either the consumer or the producer.

**Status code 500**

Although a valid message sent successfully, but was response with status code 500.
This required a review of the error. It could be due to bug or either the consumer or the producer.

**Invalid Message**

If the order xml received was invalid, there could be many reasons.
For example, XML schema changes, upstream application bug etc.