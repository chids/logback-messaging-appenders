package se.pp.gustafson.marten.logback.appender;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConnectionParameters;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.AMQP.BasicProperties;

public class AmqpAppenderTest
{

    private static final String VIRTUAL_HOST = "/";
    private static final String QUEUE = "queue";
    private static final String KEY = "key";
    private static final String EXCHANGE = "exchange";

    private static final String USER = "guest";
    private static final String PASSWORD = "guest";
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5672;

    private String message;
    private Connection conn;
    private Channel channel;
    private CountDownLatch latch;
    private LoggerContext context;

    @Test
    public void configuredWithFile()
    {
        final URL url = getClass().getClassLoader().getResource(
                getClass().getPackage().getName().replace('.', File.separatorChar) + File.separatorChar + "logback-amqp.xml");
        try
        {
            final JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(this.context);
            this.context.reset();
            configurator.doConfigure(new File(url.getFile()));
        }
        catch(final JoranException e)
        {
            fail(e.getMessage());
        }
        final Logger root = this.context.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.ALL);
        this.context.getLogger(getClass()).error(this.message);
    }

    @Test
    public void testUseLevelAsKey() throws IOException
    {
        this.channel.queueUnbind(QUEUE, EXCHANGE, KEY);
        this.channel.queueBind(QUEUE, EXCHANGE, "ERROR");
        final LoggerContext lc = createAppenderWithInlineConfiguration(true);
        lc.getLogger(getClass()).error(this.message);
    }
    
    @Test
    public void inlineConfiguration() throws Exception
    {
        final LoggerContext lc = createAppenderWithInlineConfiguration(false);
        lc.getLogger(getClass()).error(this.message);
    }

    private LoggerContext createAppenderWithInlineConfiguration(final boolean useLevelAsKey)
    {
        final AmqpAppender appender = new AmqpAppender();
        final LoggerContext lc = (LoggerContext)LoggerFactory.getILoggerFactory();
        lc.reset();
        final PatternLayout layout = new PatternLayout();
        layout.setPattern("%msg");
        layout.setContext(lc);
        layout.start();
        appender.setHost(HOST);
        appender.setPort(PORT);
        appender.setUsername(USER);
        appender.setPassword(PASSWORD);
        appender.setVirtualHost(VIRTUAL_HOST);
        appender.setExchange(EXCHANGE);
        appender.setKey(KEY);
        appender.setUseLevelAsKey(useLevelAsKey);
        appender.setLayout(layout);
        appender.start();
        appender.setContext(lc);
        final Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.ALL);
        root.addAppender(appender);
        return lc;
    }

    @Before
    public void setUp() throws Exception
    {
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
            public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) throws IOException
            {
                channel.basicAck(envelope.getDeliveryTag(), false);
                assertEquals(message, new String(body));
                latch.countDown();
            }
        });
    }

    @After
    public void tearDown() throws IOException
    {
        try
        {
            if(!latch.await(200, TimeUnit.MILLISECONDS))
            {
                fail("Timed out waiting for message");
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
