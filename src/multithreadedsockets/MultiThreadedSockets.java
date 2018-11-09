package multithreadedsockets;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.*;
import javax.persistence.EntityManager;

/**
 *
 * @author cnmoro
 */
public class MultiThreadedSockets {

    static String RASP_ADDRESS = "";
    static String LOCAL_ADDRESS = "";
    static boolean RASP_FOUND = false;
    static final int PORT = 8089;
    static double TEMPERATURE = -999;
    static Semaphore SEMAFORO;

    public static void main(String[] args) {
        //Inicializa o semáforo
        SEMAFORO = new Semaphore(1);

        //Thread 1 - Verificar novas mensagens do embarcado
        new socketServer().start();

        //Encontrar Raspberry na rede DHCP
        getRaspAddr();

        //Thread 2 - Persistir dados de temperatura no banco de dados relacional
        new databaseHandler().start();

        //Thread 3 - Enviar instrução de acionamento de componente
        //Thread 4 - Enviar instrução de desativação de componente
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
//                System.out.println("Rasp não está em " + ipAddr);
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
                            RASP_FOUND = true;
                        } else {
                            System.out.println("Recebido: " + msg);
                            
                            System.out.println("socketServer Deseja adquirir o semáforo");
                            //Adquire o semáforo
                            SEMAFORO.acquire();
                            System.out.println("socketServer Adquiriu o semáforo");

                            //Coloca o valor de temperatura recebido em uma variável
                            TEMPERATURE = Double.parseDouble(msg);

                            //Libera o semáforo 
                            SEMAFORO.release();
                            System.out.println("socketServer Liberou o semáforo");
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    static class databaseHandler extends Thread {

        public databaseHandler() {
        }

        public void run() {
            try {
                while (true) {
                    Thread.sleep(10);
                    if (TEMPERATURE != -999) {
                        System.out.println("databaseHandler Deseja adquirir o semáforo");
                        //Adquire o semáforo
                        SEMAFORO.acquire();

                        System.out.println("databaseHandler Adquiriu o semáforo");

                        //Grava o registro de temperatura no banco de dados
                        EntityManager em = EManager.getEntityManager();
                        em.getTransaction().begin();
                        em.persist(new Log(TEMPERATURE, new Date()));
                        em.getTransaction().commit();

                        //Troca o valor da variável para -999, indicando que o valor foi lido e "limpo"
                        TEMPERATURE = -999;

                        //Libera o semáforo 
                        SEMAFORO.release();
                        System.out.println("databaseHandler Liberou o semáforo");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
