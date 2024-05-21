package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;
import bgu.spl.net.impl.stomp.StompConnection;
import bgu.spl.net.impl.stomp.StompConnectionsHandler;
import bgu.spl.net.impl.stomp.StompEncoderDecoder;
import bgu.spl.net.impl.stomp.StompMessagingProtocolImpl;

public abstract class BaseServer<T> implements Server<T> {

    private final int port;
    private final Supplier<StompMessagingProtocolImpl<T>> protocolFactory;
    private final Supplier<StompEncoderDecoder<T>> encdecFactory;
    private ServerSocket sock;

    public BaseServer(
            int port,
            Supplier<StompMessagingProtocolImpl<T>> protocolFactory,
            Supplier<StompEncoderDecoder<T>> encdecFactory) {

        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
		this.sock = null;
    }

    @Override
    public void serve() {
         StompConnection <T> stomp=new StompConnection<>();
        try (ServerSocket serverSock = new ServerSocket(port)) {
			System.out.println("Server started");

            this.sock = serverSock; //just to be able to close

            while (!Thread.currentThread().isInterrupted()) {

                Socket clientSock = serverSock.accept();
                System.err.println("Client connected");
                StompMessagingProtocolImpl<T> New_Protocol = protocolFactory.get();
                StompConnectionsHandler<T> handler = new  StompConnectionsHandler<>(
                        clientSock,
                        encdecFactory.get(),
                        New_Protocol);
                  New_Protocol.start(stomp.count,stomp); //Initalise the id_socket and StompConnection;
                  stomp.addHandlers(handler); // add to Handlerconnection list and add to Hashmap(id<->Handler);
                execute(handler);
            }
        } catch (IOException ex) {
        }

        System.out.println("server closed!!!");
    }

    @Override
    public void close() throws IOException {
		if (sock != null)
			sock.close();
    }

    protected abstract void execute(StompConnectionsHandler<T>  handler);

}
