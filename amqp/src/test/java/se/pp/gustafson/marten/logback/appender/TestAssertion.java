/**
 * 
 */
package se.pp.gustafson.marten.logback.appender;

import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.AMQP.BasicProperties;

interface TestAssertion
{
    void doAssert(Envelope envelope, BasicProperties properties, byte[] body);
}