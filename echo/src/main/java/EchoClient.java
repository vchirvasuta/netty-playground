import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyStore;
import javax.net.ssl.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslHandler;

public class EchoClient {
    private final String host;
    private final int port;

    private SSLContext sslc;
    private SSLEngine clientEngine;

    private static String keyStoreFile = "D:\\Work\\_BigSearch\\SSL\\KeyStoreManager\\var\\work\\Traceability\\server\\jks\\server_keystore.jks";
    private static String trustStoreFile = "D:\\Work\\_BigSearch\\SSL\\KeyStoreManager\\var\\work\\Traceability\\server\\jks\\Traceability_truststore.jks";
    private static String passwd = "secret";
    private static String TENANT_NAME = "DELL";

    public EchoClient(String host, int port) throws Exception{
        this.host = host;
        this.port = port;

        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");

        char[] passphrase = passwd.toCharArray();

        ks.load(new FileInputStream(keyStoreFile), passphrase);
        ts.load(new FileInputStream(trustStoreFile), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);

        SSLContext sslCtx = SSLContext.getInstance("TLS");

        sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        sslc = sslCtx;

        clientEngine = sslc.createSSLEngine("client", 1111);
        clientEngine.setUseClientMode(true);
    }

    public void start() throws Exception {
        URI uri = new URI("https://127.0.0.1:1111/");
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(host, port))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();

                            p.addLast("ssl", new SslHandler(clientEngine));

                            p.addLast(new HttpClientCodec());

                            // Remove the following line if you don't want automatic content decompression.
                            p.addLast(new HttpContentDecompressor());

                            p.addLast(new EchoClientHandler());
                        }
                    });

            // Make the connection attempt.
            Channel ch = b.connect(host, port).sync().channel();

            // Prepare the HTTP request.
            HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath());
            request.headers().set(HttpHeaderNames.HOST, host);
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
            request.headers().set("TENANT", TENANT_NAME);

            // Send the HTTP request.
            ch.writeAndFlush(request);

            // Wait for the server to close the connection.
            ch.closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: " + EchoClient.class.getSimpleName() + " <host> <port>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        new EchoClient(host, port).start();
    }
}
