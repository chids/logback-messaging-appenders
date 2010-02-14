package se.pp.gustafson.marten.logback.appender;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConnectionParameters;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.AMQP.BasicProperties;

public abstract class AbstractAmqpAppenderTest
{

    static final String VIRTUAL_HOST = "/";
    static final String QUEUE = "queue";
    static final String KEY = "key";
    static final String EXCHANGE = "exchange";

    static final String USER = "guest";
    static final String PASSWORD = "guest";
    static final String HOST = "127.0.0.1";
    static final int PORT = 5672;

    String message;
    Connection conn;
    Channel channel;
    CountDownLatch latch;
    LoggerContext context;
    TestAssertion perTestAssertion;

    final void setTestAssertion(final TestAssertion perTestAssertion)
    {
        this.perTestAssertion = perTestAssertion;
    }

    final AmqpAppender createAppenderWithInlineConfiguration()
    {
        final AmqpAppender appender = new AmqpAppender();
        this.context.reset();
        final PatternLayout layout = new PatternLayout();
        layout.setPattern("%msg");
        layout.setContext(this.context);
        layout.start();
        appender.setHost(HOST);
        appender.setPort(PORT);
        appender.setUsername(USER);
        appender.setPassword(PASSWORD);
        appender.setVirtualHost(VIRTUAL_HOST);
        appender.setExchange(EXCHANGE);
        appender.setKey(KEY);
        appender.setUseLevelAsKey(false);
        appender.setUseLevelAsPriority(false);
        appender.setLayout(layout);
        appender.start();
        appender.setContext(this.context);
        return appender;
    }

    final Logger createLogger(final AmqpAppender appender)
    {
        final ch.qos.logback.classic.Logger root = this.context.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.ALL);
        root.addAppender(appender);
        return root;
    }

    @Before
    public final void setUp() throws Exception
    {
        this.perTestAssertion = null;
        this.context = (LoggerContext)LoggerFactory.getILoggerFactory();
        this.latch = new CountDownLatch(1);
        this.message = " Hello, my world is at " + System.currentTimeMillis();
        ConnectionParameters params = new ConnectionParameters();
        params.setUsername(USER);
        params.setPassword(PASSWORD);
        params.setVirtualHost(VIRTUAL_HOST);
        params.setRequestedHeartbeat(0);
        final ConnectionFactory factory = new ConnectionFactory(params);
        this.conn = factory.newConnection(HOST, PORT);
        this.channel = conn.createChannel();
        this.channel.exchangeDeclare(EXCHANGE, "direct", false);
        this.channel.queueDelete(QUEUE);
        this.channel.queueDeclare(QUEUE, false);
        this.channel.queueBind(QUEUE, EXCHANGE, KEY);
        this.channel.basicConsume(QUEUE, new QueueingConsumer(this.channel)
        {
            @Override
            public void handleDelivery(final String consumerTag, final Envelope envelope, final BasicProperties properties, final byte[] body)
                    throws IOException
            {
                channel.basicAck(envelope.getDeliveryTag(), false);
                assertEquals(message, new String(body));
                if(perTestAssertion != null)
                {
                    perTestAssertion.doAssert(envelope, properties, body);
                }
                latch.countDown();
            }
        });
    }

    @After
    public final void tearDown() throws IOException
    {
        try
        {
            if(!latch.await(3000, TimeUnit.MILLISECONDS))
            {
                fail("Timed out waiting for message, check system err for indications of test failure");
            }
        }
        catch(final InterruptedException e)
        {
            fail("Interrupted while waiting for message");
        }
        this.channel.close();
        this.conn.close();
        this.context.stop();
    }
}
