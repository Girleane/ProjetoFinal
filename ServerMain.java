import java.rmi.registry.LocateRegistry;
import java.rmi.Naming;

public class ServerMain {
    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(1099);
            ChatServerImpl servidor = new ChatServerImpl();
            Naming.rebind("ChatServer", servidor);
            System.out.println("Servidor RMI e MOM pronto.");
            // Mantém o processo vivo para continuar servindo objetos RMI
            try {
                while (true) {
                    Thread.sleep(10_000);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}