import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class ChatClient extends UnicastRemoteObject implements IClientClient {
    
    private String meuNome;
    
    public ChatClient(String nome) throws RemoteException {
        this.meuNome = nome;
    }

    public void receberMensagem(String de, String texto) throws RemoteException {
        // Req 1: Apresentação na UI
        System.out.println("\n>>> NOVA MENSAGEM de [" + de + "]: " + texto);
        System.out.print("Comando > "); // Reprint do prompt
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Digite seu nome de usuário: ");
            String nome = scanner.nextLine();

            ChatClient cliente = new ChatClient(nome);
            
            // Conexão RMI
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            IChatServer servidor = (IChatServer) registry.lookup("ChatServer");
            // Registrar usuário (cria fila no servidor de mensagens)
            try { servidor.registrarUsuario(nome); } catch (Exception ex) { /* ignore if exists */ }

            boolean rodando = true;
            boolean isOnline = false;

            while (rodando) {
                // Req 1: UI mostrando status e contatos (simplificado via console)
                System.out.println("\n--- Status: " + (isOnline ? "ONLINE" : "OFFLINE") + " ---");
                System.out.println("1. Ficar Online (Login)");
                System.out.println("2. Ficar Offline (Logout)");
                System.out.println("3. Adicionar Amigo");
                System.out.println("4. Enviar Mensagem");
                System.out.println("5. Sair da Aplicação");
                System.out.print("Comando > ");

                String op = scanner.nextLine();

                switch (op) {
                    case "1": // Req 2 e 7
                        servidor.login(nome, cliente);
                        isOnline = true;
                        break;
                    case "2": // Req 2
                        servidor.logout(nome);
                        isOnline = false;
                        break;
                    case "3": // Req 8
                        System.out.print("Nome do amigo: ");
                        String amigo = scanner.nextLine();
                        servidor.adicionarContato(nome, amigo);
                        break;
                    case "4": 
                        if (!isOnline) {
                            System.out.println("Voce precisa estar online para enviar!");
                            break;
                        }
                        // Req 1: Lista deve ser apresentada
                        System.out.println("Seus contatos: " + servidor.listarContatos(nome));
                        System.out.print("Para quem: ");
                        String para = scanner.nextLine();
                        System.out.print("Mensagem: ");
                        String msg = scanner.nextLine();
                        servidor.enviarMensagem(nome, para, msg);
                        break;
                    case "5":
                        if(isOnline) servidor.logout(nome);
                        rodando = false;
                        System.exit(0);
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}