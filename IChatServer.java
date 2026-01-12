import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IChatServer extends Remote {
    void login(String nome, IClientClient callback) throws RemoteException;
    void logout(String nome) throws RemoteException;
    void enviarMensagem(String de, String para, String texto) throws RemoteException;
    void adicionarContato(String usuario, String contato) throws RemoteException;
    List<String> listarContatos(String usuario) throws RemoteException;
    // NOVO: Retorna todos os usuários que já se registraram no servidor
    List<String> obterUsuariosGlobais() throws RemoteException;
    // Registra um novo usuário (cria fila no servidor de mensagens)
    void registrarUsuario(String nome) throws RemoteException;
    // Remove um contato da lista do usuário
    void removerContato(String usuario, String contato) throws RemoteException;
}