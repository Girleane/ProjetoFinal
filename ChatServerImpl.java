import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;
import com.rabbitmq.client.*;
import java.util.*;

public class ChatServerImpl extends UnicastRemoteObject implements IChatServer {
    private Map<String, IClientClient> usuariosOnline = new ConcurrentHashMap<>();
    private Map<String, List<String>> listaContatos = new ConcurrentHashMap<>();
    // NOVO: Registro de todos os perfis criados
    private Set<String> usuariosRegistrados = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    private Connection rabbitConn;
    private Channel rabbitChannel;
    private final static String QUEUE_PREFIX = "social_net_";

    public ChatServerImpl() throws RemoteException {
        super();
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost"); 
            rabbitConn = factory.newConnection();
            rabbitChannel = rabbitConn.createChannel();
            System.out.println("[SERVER] Conectado ao RabbitMQ.");
        } catch (Exception e) {
            System.err.println("Erro RabbitMQ: " + e.getMessage());
        }
    }

    @Override
    public synchronized void login(String nome, IClientClient callback) throws RemoteException {
        usuariosRegistrados.add(nome); // Registra o usuário globalmente
        usuariosOnline.put(nome, callback);
        
        try {
            String queueName = QUEUE_PREFIX + nome;
            rabbitChannel.queueDeclare(queueName, true, false, false, null);
            
            GetResponse response;
            while ((response = rabbitChannel.basicGet(queueName, true)) != null) {
                String msg = new String(response.getBody(), "UTF-8");
                String[] parts = msg.split(":", 2);
                callback.receberMensagem(parts[0], parts[1]);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public synchronized void registrarUsuario(String nome) throws RemoteException {
        usuariosRegistrados.add(nome);
        try {
            String queueName = QUEUE_PREFIX + nome;
            rabbitChannel.queueDeclare(queueName, true, false, false, null);
        } catch (Exception e) {
            throw new RemoteException("Erro ao criar fila no servidor de mensagens: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> obterUsuariosGlobais() throws RemoteException {
        return new ArrayList<>(usuariosRegistrados);
    }

    @Override
    public void logout(String nome) throws RemoteException {
        usuariosOnline.remove(nome);
    }

    @Override
    public void enviarMensagem(String de, String para, String texto) throws RemoteException {
        if (usuariosOnline.containsKey(para)) {
            try {
                usuariosOnline.get(para).receberMensagem(de, texto);
            } catch (RemoteException e) {
                enviarParaFila(de, para, texto);
            }
        } else {
            enviarParaFila(de, para, texto);
        }
    }

    private void enviarParaFila(String de, String para, String texto) {
        try {
            String q = QUEUE_PREFIX + para;
            rabbitChannel.queueDeclare(q, true, false, false, null);
            rabbitChannel.basicPublish("", q, com.rabbitmq.client.MessageProperties.PERSISTENT_TEXT_PLAIN, (de + ":" + texto).getBytes("UTF-8"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void adicionarContato(String usuario, String contato) throws RemoteException {
        // adiciona contato ao usuário
        listaContatos.computeIfAbsent(usuario, k -> new ArrayList<>());
        List<String> l = listaContatos.get(usuario);
        if (!l.contains(contato)) l.add(contato);
        // garante relacionamento mútuo: o contato também passa a seguir o usuário
        listaContatos.computeIfAbsent(contato, k -> new ArrayList<>());
        List<String> l2 = listaContatos.get(contato);
        if (!l2.contains(usuario)) l2.add(usuario);
    }

    @Override
    public void removerContato(String usuario, String contato) throws RemoteException {
        List<String> l = listaContatos.get(usuario);
        if (l != null) l.remove(contato);
    }

    @Override
    public List<String> listarContatos(String usuario) throws RemoteException {
        return listaContatos.getOrDefault(usuario, new ArrayList<>());
    }
}