package bgu.spl.net.impl.stomp;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class StompConnectionsHandler<T> implements ConnectionHandler<T>,Runnable{

    private final StompMessagingProtocolImpl<T> protocol;
    private final StompEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;
 
    public StompConnectionsHandler(Socket sock, StompEncoderDecoder<T> reader, StompMessagingProtocolImpl<T> protocol) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
    }

    @Override
    public void run() {
        try (Socket sock = this.sock) { //just for automatic closing
            int read;

            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());
            

            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                T nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    System.err.println("I got from a client:\n" + nextMessage);
                     protocol.process(nextMessage);
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    @Override
    public void send(T msg) {
        if (msg != null) {
            System.err.println("The message sent to client is:\n" + msg );
            try {
                String message=(String)msg;
                out.write(encdec.encode((T)message));
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }
    }


}
