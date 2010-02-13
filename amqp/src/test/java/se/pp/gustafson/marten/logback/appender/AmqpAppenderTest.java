package se.pp.gustafson.marten.logback.appender;

import static junit.framework.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URL;

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
import ch.qos.logback.core.util.StatusPrinter;

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
    private static final String Q = "q";
    private static final String KEY = "key";
    private static final String EXCHANGE = "yo";

    private static final String USER = "guest";
    private static final String USER_PASSWORD = "guest";
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5672;
    private String message;
    private Connection conn;
    private Channel channel;

    @Test
    public void configuredWithFile()
    {
        final URL url = getClass().getClassLoader().getResource(getClass().getPackage().getName().replace('.', File.separatorChar) + File.separatorChar + "logback-amqp.xml");
        final LoggerContext lc = (LoggerContext)LoggerFactory.getILoggerFactory();
        try
        {
            final JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(lc);
            lc.reset();
            configurator.doConfigure(new File(url.getFile()));
        }
        catch(JoranException je)
        {
            je.printStackTrace();
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
        final Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.ALL);
        lc.getLogger(getClass()).error(this.message);
    }

    @Test
    public void inlineConfiguration() throws Exception
    {
        final AmqpAppender appender = new AmqpAppender();
        final LoggerContext lc = (LoggerContext)LoggerFactory.getILoggerFactory();
        lc.reset();
        final PatternLayout layout = new PatternLayout();
        layout.setPattern("%msg");
        layout.setContext(lc);
        try
        {
            layout.start();
            appender.setHost(HOST);
            appender.setPort(PORT);
            appender.setUsername(USER);
            appender.setPassword(USER_PASSWORD);
            appender.setVirtualHost(VIRTUAL_HOST);
            appender.setExchange(EXCHANGE);
            appender.setKey(KEY);
            appender.setLayout(layout);//new EchoLayout<ILoggingEvent>());
            appender.start();
            appender.setContext(lc);
            final Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);
            root.setLevel(Level.ALL);
            root.addAppender(appender);
            lc.getLogger(getClass()).error(this.message);
        }
        finally
        {
            appender.stop();
            layout.stop();
        }
    }

    @Before
    public void setUp() throws Exception
    {
        this.message = " Hello, my world is at " + System.currentTimeMillis();
        ConnectionParameters params = new ConnectionParameters();
        params.setUsername("guest");
        params.setPassword("guest");
        params.setVirtualHost(VIRTUAL_HOST);
        params.setRequestedHeartbeat(0);
        ConnectionFactory factory = new ConnectionFactory(params);
        this.conn = factory.newConnection("127.0.0.1", 5672);
        this.channel = conn.createChannel();
        this.channel.exchangeDeclare(EXCHANGE, "direct", false);
        this.channel.queueDeclare(Q, false);
        this.channel.queueBind(Q, EXCHANGE, KEY);
        this.channel.basicConsume(Q, new QueueingConsumer(this.channel)
        {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) throws IOException
            {
                System.err.println(new String(body));
                channel.basicAck(envelope.getDeliveryTag(), false);
                assertEquals(message, new String(body));
            }
        });
    }

    @After
    public void tearDown() throws IOException
    {
        this.channel.queueDelete(Q).getMessageCount();
        this.channel.close();
        this.conn.close();
    }

}
