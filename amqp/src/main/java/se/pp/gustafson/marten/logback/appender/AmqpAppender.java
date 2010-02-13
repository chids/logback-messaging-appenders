package se.pp.gustafson.marten.logback.appender;

import java.io.IOException;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConnectionParameters;

public final class AmqpAppender extends AppenderBase<ILoggingEvent>
{

    private String username;
    private String password;
    private String virtualHost;
    private String host;
    private int port;
    private String exchange;
    private String key;
    private Connection conn;
    private Channel channel;

    public void start()
    {
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
        System.err.println(getClass().getSimpleName() + " configured: " + toString());
    }
    
    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("Exchange [");
        sb.append(this.exchange);
        sb.append("] at vhost [");
        sb.append(this.virtualHost);
        sb.append("] using key [");
        sb.append(this.key);
        sb.append("], username [");
        sb.append(this.username);
        sb.append("] and connected to [");
        sb.append(this.host);
        sb.append(':');
        sb.append(this.port);
        sb.append(']');
        return sb.toString();
    }

    public void stop()
    {
        try
        {
            this.channel.close();
        }
        catch(final IOException e)
        {
            e.printStackTrace();
        }
        try
        {
            this.conn.close();
        }
        catch(final IOException e)
        {
            e.printStackTrace();
        }
        super.stop();
    }

    protected void append(final ILoggingEvent event)
    {
        try
        {
            //FIXME: Support configuration for properties;
            //final BasicProperties props = new AMQP.BasicProperties();
            final byte[] payload = super.getLayout().doLayout(event).getBytes();
            this.channel.basicPublish(this.exchange, this.key, null, payload);
        }
        catch(final IOException e)
        {
            e.printStackTrace();
        }
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public void setVirtualHost(String virtualHost)
    {
        this.virtualHost = virtualHost;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public void setExchange(String exchange)
    {
        this.exchange = exchange;
    }

    public void setKey(String key)
    {
        this.key = key;
    }
}
