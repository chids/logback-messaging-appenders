package se.pp.gustafson.marten.logback.appender;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Test;
import org.slf4j.Logger;

import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.AMQP.BasicProperties;

public class AmqpAppenderTest extends AbstractAmqpAppenderTest
{
    @Test
    public void configuredWithFile()
    {
        final URL url = getClass().getClassLoader().getResource("logback-amqp.xml");
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
        root.error(this.message);
    }

    @Test
    public void testUseLevelAsKey() throws IOException
    {
        this.channel.queueUnbind(QUEUE, EXCHANGE, KEY);
        this.channel.queueBind(QUEUE, EXCHANGE, "ERROR");
        final AmqpAppender appender = createAppenderWithInlineConfiguration();
        appender.setUseLevelAsKey(true);
        createLogger(appender).error(this.message);
    }

    @Test
    public void testUseLevelAsPriority() throws IOException
    {
        setTestAssertion(new TestAssertion()
        {
            @Override
            public void doAssert(final Envelope envelope, final BasicProperties properties, final byte[] body)
            {
                assertEquals(new Integer(7), properties.priority);
            }
        });
        final AmqpAppender appender = createAppenderWithInlineConfiguration();
        appender.setUseLevelAsPriority(true);
        createLogger(appender).warn(this.message);
    }

    
    @Test
    public void inlineConfiguration() throws Exception
    {
        final org.slf4j.Logger logger = createLogger(createAppenderWithInlineConfiguration());
        logger.error(this.message);
    }
}
