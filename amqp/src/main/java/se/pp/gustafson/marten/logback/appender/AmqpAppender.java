package se.pp.gustafson.marten.logback.appender;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConnectionParameters;
import com.rabbitmq.client.AMQP.BasicProperties;

public final class AmqpAppender extends AppenderBase<ILoggingEvent>
{
    private static final Map<java.lang.String, java.lang.Object> defaultHeaders = null;
    private static final Integer defaultDeliveryMode = new Integer(1);
    private static final Integer defaultPriority = new Integer(4);
    private static final Date defaultTimestamp = null;
    private static final String defaultContentType = "text/plain";
    private static final String defaultContentEncoding = "UTF-8";
    private static final String defaultCorrelationId = null;
    private static final String defaultReplyTo = null;
    private static final String defaultExpiration = null;
    private static final String defaultMessageId = null;
    private static final String defaultType = null;
    private static final String defaultUserId = null;
    private static final String defaultAppId = null;
    private static final String defaultClusterId = null;

    private Connection conn;
    private Channel channel;
    private String username;
    private String password;
    private String virtualHost;
    private String host;
    private int port;
    private String exchange;
    private String key;
    private boolean useLevelAsKey;
    private boolean useLevelAsPriority;
    private String contentType;
    private String encoding;
    private Integer deliveryMode;

    public void start()
    {
        initalizeProperties();
        try
        {
            final ConnectionParameters params = new ConnectionParameters();
            params.setUsername(this.username);
            params.setPassword(this.password);
            params.setVirtualHost(this.virtualHost);
            params.setRequestedHeartbeat(0);
            final ConnectionFactory factory = new ConnectionFactory(params);
            this.conn = factory.newConnection(this.host, this.port);
            this.channel = conn.createChannel();
        }
        catch(final IOException e)
        {
            e.printStackTrace();
        }
        super.start();
        //System.err.println(getClass().getSimpleName() + " configured: " + toString());
    }

    private void initalizeProperties()
    {
        this.contentType = (this.contentType == null || this.contentType.trim().length() == 0) ? defaultContentType : this.contentType;
        this.encoding = (this.encoding == null || this.encoding.trim().length() == 0) ? defaultContentEncoding : this.encoding;
        this.deliveryMode = (this.deliveryMode == null) ? defaultDeliveryMode : this.deliveryMode;
    }

    public void stop()
    {
        try
        {
            this.channel.close();
        }
        catch(final Exception ignored)
        {}
        try
        {
            this.conn.close();
        }
        catch(final Exception ignored)
        {}
        super.stop();
    }

    protected void append(final ILoggingEvent event)
    {
        try
        {
            final Level level = event.getLevel();
            final byte[] payload = super.getLayout().doLayout(event).getBytes();
            final String key = (this.useLevelAsKey) ? level.toString() : this.key;
            this.channel.basicPublish(this.exchange, key, createProperties(event, level), payload);
        }
        catch(final IOException e)
        {
            e.printStackTrace();
        }
    }

    private BasicProperties createProperties(final ILoggingEvent event, final Level level)
    {
        int priority;
        try
        {
            priority = (this.useLevelAsPriority) ? LevelWithPriority.valueOf(level.toString()).intValue : defaultPriority;
        }
        catch(final IllegalArgumentException ignored)
        {
            priority = defaultPriority;
        }
        final BasicProperties props = new BasicProperties(this.contentType, this.encoding, defaultHeaders, this.deliveryMode, priority, defaultCorrelationId,
                defaultReplyTo, defaultExpiration, defaultMessageId, defaultTimestamp, defaultType, defaultUserId, defaultAppId, defaultClusterId);
        return props;
    }

    public void setUsername(final String username)
    {
        this.username = username;
    }

    public void setPassword(final String password)
    {
        this.password = password;
    }

    public void setVirtualHost(final String virtualHost)
    {
        this.virtualHost = virtualHost;
    }

    public void setHost(final String host)
    {
        this.host = host;
    }

    public void setPort(final int port)
    {
        this.port = port;
    }

    public void setExchange(final String exchange)
    {
        this.exchange = exchange;
    }

    public void setKey(final String key)
    {
        this.key = key;
    }

    public void setUseLevelAsKey(final boolean useLevelAsKey)
    {
        this.useLevelAsKey = useLevelAsKey;
    }

    public void setUseLevelAsPriority(final boolean useLevelAsPriority)
    {
        this.useLevelAsPriority = useLevelAsPriority;
    }

    public void setContentType(final String contentType)
    {
        this.contentType = contentType;
    }

    public void setEncoding(final String encoding)
    {
        this.encoding = encoding;
    }

    public void setPersistent(boolean persistent)
    {
        this.deliveryMode = (persistent) ? 1 : 2;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("Exchange [");
        sb.append(this.exchange);
        sb.append("] at vhost [");
        sb.append(this.virtualHost);
        sb.append("] using ");
        if(this.useLevelAsKey)
        {
            sb.append("log level as key");
        }
        else
        {
            sb.append("key [");
            sb.append(this.key);
            sb.append("],");
        }
        sb.append(" username [");
        sb.append(this.username);
        sb.append("] and connected to [");
        sb.append(this.host);
        sb.append(':');
        sb.append(this.port);
        sb.append(']');
        return sb.toString();
    }
}