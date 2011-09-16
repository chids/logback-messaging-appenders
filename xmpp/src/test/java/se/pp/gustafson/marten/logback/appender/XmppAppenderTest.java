package se.pp.gustafson.marten.logback.appender;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

import org.apache.vysper.mina.TCPEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCModule;
import org.apache.vysper.xmpp.server.XMPPServer;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import se.pp.gustafson.marten.logback.appender.XmppManager.Callback;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;

public class XmppAppenderTest
{
    private static final String CERT_FILE_NAME = "bogus_mina_tls.cert";
    private static final String RESOURCE = "/";
    private static final int PORT = 25222; // FIXME: Dynamic?
    private static final String XMPP_DOMAIN = "127.0.0.1";
    private static final String XMPP_MUC_CHAT_DOMAIN = "conference";
    private static final String CHAT_NAME = "muc@" + XMPP_MUC_CHAT_DOMAIN + '.' + XMPP_DOMAIN;
    private static final String SERVER_CERT_PASSWORD = "boguspw";
    static final Entity ADMIN = EntityImpl.parseUnchecked("admin@" + XMPP_DOMAIN);
    private static final String ADMIN_PASSWORD = "admin";
    static final Entity USER = EntityImpl.parseUnchecked("user@" + XMPP_DOMAIN);
    private static final String USER_PASSWORD = "user";
    XMPPServer server;
    final CountDownLatch serverTermination = new CountDownLatch(1);
    final CountDownLatch serverStart = new CountDownLatch(1);

    /**
     * Fires up an embedded Vysper instance, logs a message with the XMPP appender and asserts
     * that the message was sent to the configured chat room.
     */
    @Test
    public void testAppender() throws Exception
    {
        final XmppManager.Callback callback = mock(XmppManager.Callback.class);
        this.serverStart.await();
        try
        {
            final XmppManager asserter = x(callback);
            final LoggerContext ctx = (LoggerContext)LoggerFactory.getILoggerFactory();
            final XmppAppender appender = configureAppender(ctx);
            ctx.reset();
            ctx.getLogger(getClass()).addAppender(appender);
            ctx.getLogger(getClass()).error("test");
            Thread.sleep(1000);
            ctx.stop();
            asserter.disconnect();
        }
        finally
        {
            this.serverTermination.countDown();
        }
        verify(callback, times(1)).invoke(any(Message.class));
    }

    private static XmppManager x(final Callback callback) throws XMPPException
    {
        final XmppManager client = new XmppManager(XMPP_DOMAIN, PORT);
        client.connectAndLogin(ADMIN.getFullQualifiedName(), ADMIN_PASSWORD, RESOURCE);
        client.joinChat("asserter", CHAT_NAME);
        client.addListener(Message.class, callback);
        return client;
    }

    private XmppAppender configureAppender(final LoggerContext ctx)
    {
        final XmppAppender appender = new XmppAppender();
        appender.setBotName(getClass().getSimpleName());
        appender.setChatName(CHAT_NAME);
        appender.setPort(PORT);
        appender.setServer("127.0.0.1");
        appender.setUser(USER.getFullQualifiedName());
        appender.setPassword(USER_PASSWORD);
        appender.setResource(RESOURCE);
        final PatternLayout layout = new PatternLayout();
        layout.setPattern("%msg");
        appender.start();
        return appender;
    }

    @Before
    public void startServer() throws Exception
    {
        new Thread(getClass().getSimpleName() + " - server")
        {
            @Override
            public void run()
            {
                try
                {
                    final URL url = getClass().getClassLoader().getResource(CERT_FILE_NAME);
                    XmppAppenderTest.this.server = new XMPPServer(XMPP_DOMAIN);
                    final StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();
                    final AccountManagement accountManagement = (AccountManagement)providerRegistry.retrieve(AccountManagement.class);
                    accountManagement.addUser(ADMIN, ADMIN_PASSWORD);
                    accountManagement.addUser(USER, USER_PASSWORD);
                    final TCPEndpoint endpoint = new TCPEndpoint();
                    endpoint.setPort(PORT);
                    XmppAppenderTest.this.server.addEndpoint(endpoint);
                    XmppAppenderTest.this.server.setTLSCertificateInfo(new File(url.getFile()), SERVER_CERT_PASSWORD);
                    XmppAppenderTest.this.server.setStorageProviderRegistry(providerRegistry);
                    XmppAppenderTest.this.server.start();
                    XmppAppenderTest.this.server.addModule(new MUCModule(XMPP_MUC_CHAT_DOMAIN));
                    XmppAppenderTest.this.serverStart.countDown();
                    XmppAppenderTest.this.serverTermination.await();
                }
                catch(final Exception e)
                {
                    XmppAppenderTest.this.serverTermination.countDown();
                    throw new RuntimeException(e.getMessage(), e);
                }
                finally
                {
                    XmppAppenderTest.this.serverStart.countDown();
                }
            }
        }.start();
    }

    @After
    public void stopServer()
    {
        if(this.server != null)
        {
            this.server.stop();
        }
    }

}
