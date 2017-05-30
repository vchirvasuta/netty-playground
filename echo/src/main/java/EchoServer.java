import java.io.*;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import javax.net.ssl.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;

public class EchoServer {
    private final int port;

    private SSLContext sslc;
    private SSLEngine serverEngine;

    private static String keyStoreFile = "D:\\Work\\_BigSearch\\SSL\\KeyStoreManager\\var\\work\\Traceability\\server\\jks\\server_keystore.jks";
    private static String trustStoreFile = "D:\\Work\\_BigSearch\\SSL\\KeyStoreManager\\var\\work\\Traceability\\server\\jks\\Traceability_truststore.jks";
    private static String passwd = "secret";



    public EchoServer(int port) throws Exception{

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

        serverEngine = sslc.createSSLEngine();
        serverEngine.setUseClientMode(false);
        serverEngine.setNeedClientAuth(true);
    }


    public static void main (String[] args) throws Exception {
        System.setProperty("javax.net.debug", "all");
        if (args.length != 1) {
            System.err.println("Usage: " + EchoServer.class.getSimpleName() + " <port>");
        }
        int port = Integer.parseInt(args[0]);
        new EchoServer(port).start();

    }

    private void start() throws Exception {
        final EchoServerHandler serverHandler = new EchoServerHandler();
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group).channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();

                            p.addLast("ssl", new SslHandler(serverEngine));

                            p.addLast(new HttpRequestDecoder());
                            p.addLast(new HttpResponseEncoder());

                            p.addLast(serverHandler);
                        }
                    });
            Channel ch = b.bind(1111).sync().channel();

            System.err.println("Open your web browser and navigate to https://127.0.0.1:" + 1111 + '/');

            ch.closeFuture().sync();

        } finally {
            group.shutdownGracefully().sync();
        }

    }
}
