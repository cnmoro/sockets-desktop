package multithreadedsockets;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 *
 * @author cnmoro
 */
public class MultiThreadedSockets {

    static String RASP_ADDRESS = "";
    static String LOCAL_ADDRESS = "";
    static final int PORT = 8089;

    public static void main(String[] args) {
        new socketServer().start();
        getRaspAddr();
    }

    public static String getLocalAddr() {
        String ip = "";
        try {
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface n = (NetworkInterface) e.nextElement();
                Enumeration ee = n.getInetAddresses();
                while (ee.hasMoreElements()) {
                    InetAddress i = (InetAddress) ee.nextElement();
                    if (!i.getHostAddress().contains("127.0.0.1") && !i.getHostAddress().contains(":") && !i.getHostAddress().contains("%")) {
                        ip = i.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ip;
    }

    public static void getRaspAddr() {
        String localAddress = getLocalAddr();
        LOCAL_ADDRESS = localAddress;

        System.out.println("IP Local: " + localAddress);
        String addrParts[] = localAddress.split("\\.");

        if (addrParts.length == 4) {
            String addrProto = addrParts[0] + "." + addrParts[1] + "." + addrParts[2] + ".";

            System.out.println("Procurando pelo raspberry na rede...");

            for (int i = 1; i <= 255; i++) {
                if (!(addrProto + i).equals(LOCAL_ADDRESS)) {
                    new raspSocketFinder(addrProto + i).start();
                }
            }
        }
    }

    static class raspSocketFinder extends Thread {

        Socket socket;
        String ipAddr;

        public raspSocketFinder(String ipAddr) {
            try {
                this.ipAddr = ipAddr;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                Thread.sleep(1000);

                this.socket = new Socket(ipAddr, PORT);
                System.out.println("Tentando " + ipAddr);
                OutputStream output = socket.getOutputStream();
                output.write("rasp?".getBytes());
            } catch (Exception e) {
//                System.out.println("rasp not in " + ipAddr);
            }
        }
    }

    static class socketServer extends Thread {

        ServerSocket server;

        public socketServer() {
            try {
                this.server = new ServerSocket(PORT);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                InputStream in;

                while (true) {
                    Socket socket = server.accept();

                    socket.getRemoteSocketAddress().toString();

                    InputStreamReader isr = new InputStreamReader(socket.getInputStream());
                    BufferedReader br = new BufferedReader(isr);

                    String msg = br.readLine();
                    if (msg != null) {
                        if (msg.equals("rasp!")) {
                            String addr = socket.getRemoteSocketAddress().toString();
                            addr = addr.replace("/", "");
                            addr = addr.split("\\:")[0];

                            RASP_ADDRESS = addr;
                            System.out.println("Raspberry encontrado em " + RASP_ADDRESS);
                        } else {
                            System.out.println("Recebido: " + msg);
                        }
                    }
                }
            } catch (Exception e) {

            }
        }
    }
}
