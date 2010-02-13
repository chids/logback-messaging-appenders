package se.pp.gustafson.marten.logback.appender;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import org.apache.vysper.mina.TCPEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCModule;
import org.apache.vysper.xmpp.server.XMPPServer;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.layout.EchoLayout;

public class XmppAppenderTest
{

    private static final String RESOURCE = "testresource";
    private static final int PORT = 25222; // FIXME: Dynamic?
    private static final String XMPP_DOMAIN = "127.0.0.1";
    private static final String XMPP_MUC_CHAT_DOMAIN = "conference";
    private static final String CERT_BASE_PATH = "src/test/config/";
    private static final String SERVER_CERT_PATH = CERT_BASE_PATH + "server.jks";
    private static final String SERVER_CERT_PASSWORD = "123456";
    private static final String ADMIN = "admin@" + XMPP_DOMAIN;
    private static final String ADMIN_PASSWORD = "admin";
    private static final String USER = "user@" + XMPP_DOMAIN;
    private static final String USER_PASSWORD = "user";
    private XMPPServer server;
    private final CountDownLatch serverTermination = new CountDownLatch(1);
    private final CountDownLatch serverStart = new CountDownLatch(1);

    @Test
    public void noop()
    {}
    
    public void x() throws Exception
    {
        serverStart.await();
        final XmppAppender appender = new XmppAppender();
        try
        {
            appender.setBotName(getClass().getSimpleName());
            appender.setChatName("muc@" + XMPP_MUC_CHAT_DOMAIN + '.' + XMPP_DOMAIN); // "muc@conference.127.0.0.1");
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
        }
        finally
        {
            //appender.stop();
        }
        System.in.read();
        appender.stop();
        serverTermination.countDown();
    }

    public void startServer() throws Exception
    {
        new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                try
                {
                    server = new XMPPServer(XMPP_DOMAIN);
                    final StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();
                    final AccountManagement accountManagement = (AccountManagement)providerRegistry.retrieve(AccountManagement.class);
                    accountManagement.addUser(ADMIN, ADMIN_PASSWORD);
                    accountManagement.addUser(USER, USER_PASSWORD);
                    final TCPEndpoint endpoint = new TCPEndpoint();
                    endpoint.setPort(PORT);
                    server.addEndpoint(endpoint);
                    server.setTLSCertificateInfo(new File(SERVER_CERT_PATH), SERVER_CERT_PASSWORD);
                    server.setStorageProviderRegistry(providerRegistry);
                    server.start();
                    server.addModule(new MUCModule(XMPP_MUC_CHAT_DOMAIN));
                    serverStart.countDown();
                    serverTermination.await();
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void stopServer()
    {
        this.server.stop();
    }

}
