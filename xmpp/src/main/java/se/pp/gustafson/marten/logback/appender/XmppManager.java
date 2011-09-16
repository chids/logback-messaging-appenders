package se.pp.gustafson.marten.logback.appender;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.MultiUserChat;

public final class XmppManager
{
    @Deprecated
    private static final int SEND_QUEUE_BACKLOG = 100; // TODO: Make this configurable
    private final XMPPConnection connection;
    private MultiUserChat chat;
    private SendThread sendThread;

    public XmppManager(final String server, final int port)
    {
        final ConnectionConfiguration config = new ConnectionConfiguration(server, port);
        config.setReconnectionAllowed(true);
        this.connection = new XMPPConnection(config);
    }

    public void connectAndLogin(final String username, final String password, final String resource) throws XMPPException
    {
        if(!this.connection.isConnected() || !this.connection.isAuthenticated())
        {
            disconnect();
            this.connection.connect();
            this.connection.login(username, password, resource);
        }
    }

    public void addListener(final Class<? extends Packet> packetType, final Callback callback)
    {
        this.connection.addPacketListener(new PacketListener()
        {
            @Override
            public void processPacket(final Packet packet)
            {
                callback.invoke(packet);
            }
        }, new PacketFilter()
        {
            @Override
            public boolean accept(final Packet packet)
            {
                return packetType.isInstance(packet);
            }
        });
    }

    public static interface Callback
    {
        void invoke(Packet packet);
    }

    public void joinChat(final String nickName, final String chatName) throws XMPPException
    {
        leaveChat();
        this.chat = new MultiUserChat(this.connection, chatName);
        this.chat.join(nickName);
        this.sendThread = new SendThread(this.chat, SEND_QUEUE_BACKLOG);
        this.sendThread.start();
    }

    /**
     * Leaves chat and disconnects
     */
    public void disconnect()
    {
        leaveChat();
        this.connection.disconnect();
    }

    public void leaveChat()
    {
        if(this.chat != null)
        {
            this.chat.leave();
        }
        if(this.sendThread != null)
        {
            this.sendThread.interrupt();
        }
    }

    public void sendMessage(final String message)
    {
        this.sendThread.enqueue(message);
    }
}