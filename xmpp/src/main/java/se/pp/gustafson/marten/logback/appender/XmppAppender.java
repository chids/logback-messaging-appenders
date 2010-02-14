package se.pp.gustafson.marten.logback.appender;

import java.util.concurrent.CountDownLatch;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.muc.MultiUserChat;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public final class XmppAppender extends AppenderBase<ILoggingEvent>
{
    private String server;
    private int port;
    private String user;
    private String password;
    private String resource;
    private XMPPConnection connection;
    private MultiUserChat muc;
    private String botName;
    private String chatName;
    private final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void start()
    {
        try
        {
            final ConnectionConfiguration config = new ConnectionConfiguration(server, port);

            // Vyper
            config.setCompressionEnabled(false);
            config.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
            config.setSASLAuthenticationEnabled(true);
            config.setDebuggerEnabled(false);

            connection = new XMPPConnection(config);
            connection.connect();
            connection.login(user, password, resource);
            System.err.println("Init chat");
            doChat();
            System.err.println("Chat initialized");
            latch.await();
            System.err.println("Chat thread terminated");
        }
        catch(final Exception e)
        {
            e.printStackTrace();
            return;
        }
        super.start();
    }

    private void doChat() throws XMPPException
    {
        this.muc = new MultiUserChat(this.connection, this.chatName);
        this.muc.join(this.botName);
        this.connection.addPacketListener(new PacketListener()
        {
            @Override
            public void processPacket(final Packet packet)
            {
                final Message message = (Message)packet;
                // FIXME: Implement command handling    
                if("?".equals(message.getBody().trim()))
                {
                    final Message reply = new Message(message.getFrom());
                    reply.setBody("Currently no help is available");
                    connection.sendPacket(reply);
                }
            }
        }, new PacketFilter()
        {
            @Override
            public boolean accept(final Packet packet)
            {
                if(packet instanceof Message)
                {
                    final Message message = (Message)packet;
                    for(final PacketExtension s : message.getExtensions())
                    {
                        if(s.getNamespace().endsWith("xhtml-im"))
                        {
                            return true;
                        }
                    }
                }
                return false;
            }
        });
        try
        {
            int i = 0;
            while(true)
            {
                this.muc.sendMessage("Ping " + i++);
                System.err.println("Ping " + i + " sent");
                Thread.sleep(500);
            }
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void stop()
    {
        latch.countDown();
        try
        {
            if(this.muc == null)
            {
                System.err.println("You may ignore this but chat object was unexpecedly null in " + getClass().getName());
            }
            else
            {
                this.muc.leave();
            }
        }
        catch(final Exception ignored)
        {
            System.err.println(ignored.getMessage());
            ignored.printStackTrace(System.err);
        }
        try
        {
            this.connection.disconnect();
        }
        finally
        {
            super.stop();
        }
    }

    @Override
    protected void append(final ILoggingEvent event)
    {
        try
        {
            final Message m = new Message("user@127.0.0.1");
            m.setBody("yeah");
            this.connection.sendPacket(m);
            this.muc.sendMessage(super.getLayout().doLayout(event));
        }
        catch(final XMPPException e)
        {
            // FIXME: ...
            e.printStackTrace();
        }
    }

    public void setServer(String server)
    {
        this.server = server;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public void setResource(String resource)
    {
        this.resource = resource;
    }

    public void setBotName(String botName)
    {
        this.botName = botName;
    }

    public void setChatName(String chatName)
    {
        this.chatName = chatName;
    }
}