package se.pp.gustafson.marten.logback.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEventVO;
import ch.qos.logback.core.AppenderBase;

public final class XmppAppender extends AppenderBase<ILoggingEvent>
{
    private String server;
    private int port;
    private String user;
    private String password;
    private String resource;
    private String botName;
    private String chatName;
    private XmppManager xmpp;

    @Override
    public void start()
    {
        try
        {
            this.xmpp = new XmppManager(this.server, this.port);
            this.xmpp.connectAndLogin(this.user, this.password, this.resource);
            this.xmpp.joinChat(this.botName, this.chatName);
        }
        catch(final Exception e)
        {
            e.printStackTrace();
            return;
        }
        super.start();
    }

    @Override
    public void stop()
    {
        try
        {
            this.xmpp.disconnect();
        }
        catch(final Exception ignored)
        {
            System.err.println("Ignored exception:");
            ignored.printStackTrace(System.err);
        }
        finally
        {
            super.stop();
        }
    }

    @Override
    protected void append(final ILoggingEvent event)
    {
        final LoggingEventVO eventVO = LoggingEventVO.build(event);
        this.xmpp.sendMessage(eventVO.getFormattedMessage());
    }

    public void setServer(final String server)
    {
        this.server = server;
    }

    public void setPort(final int port)
    {
        this.port = port;
    }

    public void setUser(final String user)
    {
        this.user = user;
    }

    public void setPassword(final String password)
    {
        this.password = password;
    }

    public void setResource(final String resource)
    {
        this.resource = resource;
    }

    public void setBotName(final String botName)
    {
        this.botName = botName;
    }

    public void setChatName(final String chatName)
    {
        this.chatName = chatName;
    }
}