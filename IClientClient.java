import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IClientClient extends Remote {
    void receberMensagem(String de, String texto) throws RemoteException;
}
