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

    static final int THREAD_SLEEP_ROUTINE_MS = 10; //10 milisegundos
    static final int THREAD_SLEEP_START_MS = 1000; //1 segundo
    static final int THREAD_SLEEP_RETRY_RASP_MS = 10000; //10 segundos
    static String RASP_ADDRESS = "";
    static String LOCAL_ADDRESS = "";
    static boolean RASP_FOUND = false;
    static final int PORT = 8089;
    static double TEMPERATURE = -999;
    static double UMIDITY = -999;
    static final double HIGH_TEMP_THRESHOLD = 25;
    static boolean SHOULD_ACTIVATE_COOLER = false;
    static boolean SHOULD_DEACTIVATE_COOLER = false;
//    static int COOLER_STATUS_COUNT = 0;
    static Semaphore SEMAFORO;

    public static void main(String[] args) {
        //Inicializa o semáforo
        SEMAFORO = new Semaphore(1);

        //Thread 1 - Verificar novas mensagens do embarcado
        new socketServer().start();

        //Encontrar Raspberry na rede DHCP
        new raspAddrManager().start();

        //Thread 2 - Persistir dados de temperatura no banco de dados relacional
        new databaseHandler().start();

        //Thread 3 - Enviar instrução de acionamento de componente
        new acionaCoolerHandler().start();

        //Thread 4 - Enviar instrução de desativação de componente
        new desativaCoolerHandler().start();
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

    static class raspAddrManager extends Thread {

        public raspAddrManager() {
        }

        public void run() {
            try {
                while (RASP_FOUND == false) {
                    String localAddress = getLocalAddr();
                    LOCAL_ADDRESS = localAddress;

                    System.out.println("IP Local: " + localAddress + "\n");
                    String addrParts[] = localAddress.split("\\.");

                    if (addrParts.length == 4) {
                        String addrProto = addrParts[0] + "." + addrParts[1] + "." + addrParts[2] + ".";

                        System.out.println("Procurando pelo raspberry na rede...\n");

                        for (int i = 1; i <= 255; i++) {
                            if (!(addrProto + i).equals(LOCAL_ADDRESS)) {
                                new raspSocketFinder(addrProto + i).start();
                            }
                        }
                    }

                    Thread.sleep(THREAD_SLEEP_RETRY_RASP_MS);
                }
            } catch (Exception e) {
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
                Thread.sleep(THREAD_SLEEP_START_MS);

                this.socket = new Socket(ipAddr, PORT);
                System.out.println("Tentando " + ipAddr + "\n");
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
                            System.out.println("Raspberry encontrado em " + RASP_ADDRESS + "\n");
                            RASP_FOUND = true;
                        } else if (msg.contains("dados")) {
                            try {
                                String data = msg;
                                data = data.substring(7);

                                double parsedTemp;
                                double parsedUmid;
                                boolean parseOk = true;

                                try {
                                    parsedTemp = Double.parseDouble(data.split(",")[0]);
                                    parsedUmid = Double.parseDouble(data.split(",")[1]);
                                } catch (NumberFormatException nfe) {
                                    parsedTemp = -999;
                                    parsedUmid = -999;
                                    parseOk = false;
                                }

                                if (parseOk) {
                                    System.out.println("socketServer Deseja adquirir o semáforo\n");

                                    //Adquire o semáforo
                                    SEMAFORO.acquire();
                                    System.out.println("socketServer Adquiriu o semáforo\n");

                                    //Coloca o valor de temperatura recebido em uma variável
                                    TEMPERATURE = parsedTemp;
                                    UMIDITY = parsedUmid;

                                    if (TEMPERATURE >= HIGH_TEMP_THRESHOLD) {
//                                    if (SHOULD_ACTIVATE_COOLER == false) {
                                        SHOULD_ACTIVATE_COOLER = true;
//                                        COOLER_STATUS_COUNT = 1;
//                                    }
                                    } else {
//                                    if (SHOULD_DEACTIVATE_COOLER == false && COOLER_STATUS_COUNT == 1) {
                                        SHOULD_DEACTIVATE_COOLER = true;
//                                        COOLER_STATUS_COUNT = 0;
//                                    }
                                    }

                                    //Libera o semáforo 
                                    SEMAFORO.release();
                                    System.out.println("socketServer Liberou o semáforo\n");
                                }
                            } catch (Exception e) {
                                System.out.println("Erro ao ler dados de temperatura e umidade.");
                            }
                        } else {
                            System.out.println("Recebido: " + msg);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class databaseHandler extends Thread {

        public databaseHandler() {
        }

        public void run() {
            try {
                while (true) {
                    Thread.sleep(THREAD_SLEEP_ROUTINE_MS);
                    if (TEMPERATURE != -999 && UMIDITY != -999) {
                        System.out.println("databaseHandler Deseja adquirir o semáforo\n");
                        //Adquire o semáforo
                        SEMAFORO.acquire();

                        System.out.println("databaseHandler Adquiriu o semáforo\n");

                        //Grava o registro de temperatura no banco de dados
                        EntityManager em = EManager.getEntityManager();
                        em.getTransaction().begin();
                        em.persist(new Log(TEMPERATURE, UMIDITY, new Date()));
                        em.getTransaction().commit();

                        //Troca o valor da variável para -999, indicando que o valor foi lido e "limpo"
                        TEMPERATURE = -999;
                        UMIDITY = -999;

                        //Libera o semáforo 
                        SEMAFORO.release();
                        System.out.println("databaseHandler Liberou o semáforo\n");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class acionaCoolerHandler extends Thread {

        Socket socket;

        public acionaCoolerHandler() {
        }

        public void run() {
            try {
                while (true) {
                    Thread.sleep(THREAD_SLEEP_ROUTINE_MS);
                    if (SHOULD_ACTIVATE_COOLER) {
                        System.out.println("acionaCoolerHandler Deseja adquirir o semáforo\n");
                        //Adquire o semáforo
                        SEMAFORO.acquire();

                        System.out.println("acionaCoolerHandler Adquiriu o semáforo\n");

                        //Envia sinal de acionamento de cooler para o raspberry
                        this.socket = new Socket(RASP_ADDRESS, PORT);
                        System.out.println("Enviando sinal de acionamento de cooler\n");
                        OutputStream output = socket.getOutputStream();
                        output.write("acionarCooler".getBytes());

                        SHOULD_ACTIVATE_COOLER = false;

                        //Libera o semáforo 
                        SEMAFORO.release();
                        System.out.println("acionaCoolerHandler Liberou o semáforo\n");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class desativaCoolerHandler extends Thread {

        Socket socket;

        public desativaCoolerHandler() {
        }

        public void run() {
            try {
                while (true) {
                    Thread.sleep(THREAD_SLEEP_ROUTINE_MS);
//                    if (SHOULD_DEACTIVATE_COOLER && COOLER_STATUS_COUNT == 0) {
                    if (SHOULD_DEACTIVATE_COOLER) {
                        System.out.println("desativaCoolerHandler Deseja adquirir o semáforo\n");
                        //Adquire o semáforo
                        SEMAFORO.acquire();

                        System.out.println("desativaCoolerHandler Adquiriu o semáforo\n");

                        //Envia sinal de desligamento de cooler para o raspberry
                        this.socket = new Socket(RASP_ADDRESS, PORT);
                        System.out.println("Enviando sinal de desligamento de cooler\n");
                        OutputStream output = socket.getOutputStream();
                        output.write("desligarCooler".getBytes());

                        SHOULD_DEACTIVATE_COOLER = false;

                        //Libera o semáforo 
                        SEMAFORO.release();
                        System.out.println("desativaCoolerHandler Liberou o semáforo\n");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
