package se.pp.gustafson.marten.logback.appender;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConnectionParameters;
import com.rabbitmq.client.GetResponse;

public class Mainer
{

    private static final String Q = "q";
    private static final String KEY = "key";
    private static final String EXCHANGE = "yo";

    public static void main(String[] args) throws Exception
    {
        ConnectionParameters params = new ConnectionParameters();
        params.setUsername("guest");
        params.setPassword("guest");
        params.setVirtualHost("/");
        params.setRequestedHeartbeat(0);
        ConnectionFactory factory = new ConnectionFactory(params);
        Connection conn = factory.newConnection("127.0.0.1", 5672);
        Channel channel = conn.createChannel();
        channel.exchangeDeclare(EXCHANGE, "direct");
        channel.queueDeclare(Q);
        channel.queueBind(Q, EXCHANGE, KEY);
        channel.basicPublish(EXCHANGE, KEY, null, "Hello, world!".getBytes());
        GetResponse response = channel.basicGet(Q, true);
        System.err.println(new String(response.getBody()));
        channel.close();
        conn.close();
    }
}
