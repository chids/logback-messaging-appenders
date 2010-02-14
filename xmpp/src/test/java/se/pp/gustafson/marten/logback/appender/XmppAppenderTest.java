package se.pp.gustafson.marten.logback.appender;

import java.io.File;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

import org.apache.vysper.mina.TCPEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCModule;
import org.apache.vysper.xmpp.server.XMPPServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.layout.EchoLayout;

public class XmppAppenderTest
{

    private static final String CERT_FILE_NAME = "bogus_mina_tls.cert";
    private static final String RESOURCE = "/";
    private static final int PORT = 25222; // FIXME: Dynamic?
    private static final String XMPP_DOMAIN = "127.0.0.1";
    private static final String XMPP_MUC_CHAT_DOMAIN = "conference";
    private static final String SERVER_CERT_PASSWORD = "boguspw";
    private static final String ADMIN = "admin@" + XMPP_DOMAIN;
    private static final String ADMIN_PASSWORD = "admin";
    private static final String USER = "user@" + XMPP_DOMAIN;
    private static final String USER_PASSWORD = "user";
    private XMPPServer server;
    private final CountDownLatch serverTermination = new CountDownLatch(1);
    private final CountDownLatch serverStart = new CountDownLatch(1);

    @Test
    public void x() throws Exception
    {
        this.serverStart.await();
        final XmppAppender appender = new XmppAppender();
        appender.setBotName(getClass().getSimpleName());
        appender.setChatName("muc@" + XMPP_MUC_CHAT_DOMAIN + '.' + XMPP_DOMAIN);
        appender.setPort(PORT);
        appender.setServer("127.0.0.1");
        appender.setUser(USER);
        appender.setPassword(USER_PASSWORD);
        appender.setResource(RESOURCE);
        PatternLayout layout = new PatternLayout();
        layout.setPattern("%msg");
        appender.setLayout(new EchoLayout<ILoggingEvent>());
        appender.start();
        final LoggerContext lc = (LoggerContext)LoggerFactory.getILoggerFactory();
        lc.reset();
        appender.setContext(lc);
        final Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(appender);
        lc.getLogger(getClass()).error("test");
        System.err.println("Done");
        System.in.read();
        appender.stop();
        this.serverTermination.countDown();
    }

    @Before
    public void startServer() throws Exception
    {
        new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                try
                {
                    final URL url = getClass().getClassLoader().getResource(CERT_FILE_NAME);
                    server = new XMPPServer(XMPP_DOMAIN);
                    final StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();
                    final AccountManagement accountManagement = (AccountManagement)providerRegistry.retrieve(AccountManagement.class);
                    accountManagement.addUser(ADMIN, ADMIN_PASSWORD);
                    accountManagement.addUser(USER, USER_PASSWORD);
                    final TCPEndpoint endpoint = new TCPEndpoint();
                    endpoint.setPort(PORT);
                    server.addEndpoint(endpoint);
                    server.setTLSCertificateInfo(new File(url.getFile()), SERVER_CERT_PASSWORD);
                    server.setStorageProviderRegistry(providerRegistry);
                    server.start();
                    server.addModule(new MUCModule(XMPP_MUC_CHAT_DOMAIN));
                    serverStart.countDown();
                    serverTermination.await();
                }
                catch(final Exception e)
                {
                    serverTermination.countDown();
                    throw new RuntimeException(e.getMessage(), e);
                }
                finally
                {
                    serverStart.countDown();
                }
            }
        }).start();
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
