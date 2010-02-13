package se.pp.gustafson.marten.logback.appender;

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
    private String trustStore;
    private String trustStorePassword;

    @Override
    public void start()
    {
        try
        {
            final ConnectionConfiguration config = new ConnectionConfiguration(this.server, this.port);
            // FIXME: ...

            // Vyper
            config.setCompressionEnabled(false);
            config.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
            config.setSASLAuthenticationEnabled(true);
            config.setDebuggerEnabled(false);
            //config.setTruststorePath(this.trustStore);
            //config.setTruststorePassword(this.trustStorePassword);

            this.connection = new XMPPConnection(config);
            this.connection.connect();
            System.err.printf("Trying login with %s %s %s", this.user, this.password, this.resource);
            this.connection.login(this.user, this.password, this.resource);
            //doChat();
        }
        catch(final XMPPException e)
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
                if(packet instanceof Message)
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
        /*
                    this.muc.addParticipantListener(new PacketListener()
                    {
                        @Override
                        public void processPacket(final Packet message)
                        {
                        System.err.println("Participant: " + message.toXML());
                        final Message welcome = new Message(message.getFrom());
                        welcome.setBody("Welcome! Status is....");
                        conn.sendPacket(welcome);
                        }
                    });
                     */
    }

    @Override
    public void stop()
    {
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
        // FIXME: Implement configurable flood protection / buffer / aggregation / ...
        try
        {
            final Message m = new Message("user2@127.0.0.1");
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

    public void setTrustStore(String trustStore)
    {
        this.trustStore = trustStore;
    }

    public void setTrustStorePassword(String trustStorePassword)
    {
        this.trustStorePassword = trustStorePassword;
    }

}