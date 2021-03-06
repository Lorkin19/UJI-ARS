package common;

import javafx.collections.ObservableList;
import modelo.Pregunta;
import modelo.Proyector;
import modelo.Sesion;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IProfesor extends Remote {
    void crearSesion(int numPreguntas) throws RemoteException;

    Pregunta crearPregunta() throws RemoteException;

    Proyector crearPartida(String sesion, IServidorInicio servidor) throws RemoteException;

    void empezarPartida() throws RemoteException;

    void pasarDePregunta() throws RemoteException;

    String getPassword()throws RemoteException;

    void verResultadosPartida() throws RemoteException;

    void finalizarPartida() throws RemoteException;

    void cargarSesiones(List<String> sesionesProfesor) throws RemoteException;

    void muestraEnunciado(String enunciado) throws RemoteException;
}
