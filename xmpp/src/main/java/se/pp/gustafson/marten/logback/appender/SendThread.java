package se.pp.gustafson.marten.logback.appender;

import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.MultiUserChat;

public final class SendThread extends Thread
{
    private final MultiUserChat chat;
    private final SloppyCircularBuffer<String> queue;

    public SendThread(final MultiUserChat chat, final int backlog)
    {
        this.queue = new SloppyCircularBuffer<String>(backlog);
        this.chat = chat;
        super.setDaemon(true);
        super.setName(getClass().getSimpleName());
    }

    public void enqueue(final String message)
    {
        this.queue.enqueue(message);
    }

    @Override
    public void run()
    {
        try
        {
            String message;
            while(this.chat.isJoined() && !isInterrupted())
            {
                int counter = 0;
                while(!isInterrupted() && null != (message = this.queue.dequeue(10, TimeUnit.MILLISECONDS)))
                {
                    send(message);
                    counter = yieldEveryHundredIteration(counter++);
                }
                Thread.sleep(500);
            }
        }
        catch(final InterruptedException exitSignal)
        {}
    }

    private static int yieldEveryHundredIteration(int counter)
    {
        if(0 == (counter % 100))
        {
            Thread.yield();
            return 0;
        }
        return counter;
    }

    private void send(final String message)
    {
        try
        {
            this.chat.sendMessage(message);
        }
        catch(final XMPPException e)
        {
            System.err.println("Ignored:");
            e.printStackTrace(System.err);
        }
    }
}
