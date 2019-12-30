package rmi;

import common.IGestionBBDD;
import javafx.collections.FXCollections;
import modelo.Pregunta;
import modelo.Profesor;
import modelo.Respuesta;
import modelo.Sesion;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GestionBBDD extends UnicastRemoteObject implements IGestionBBDD {

    GestionBBDD() throws RemoteException {

    }

    /**
     * Se realiza una conexion con la base de datos.
     *
     * @return La conexion con la base de datos.
     */
    private Connection conecta() {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection connection = DriverManager.getConnection("jdbc:sqlite:UJIARS\\UJIARSdb.db");
            return connection;

        } catch (SQLException | ClassNotFoundException ex) {
            System.out.println("Error en la conexión de la base de datos");
            return null;
        }
    }

    /**
     * Registramos el profesor en la base de datos
     *
     * @param usuario  Nombre de usuario del profesor
     * @param password Contrasenya del profesor
     * @throws RemoteException Si hay un error en la conexion remota
     */
    @Override
    public boolean registraProfesor(String usuario, String password) throws RemoteException {
        try {
            Connection connection = conecta();
            String sentencia = "INSERT INTO profesor VALUES (?,?,?)";
            PreparedStatement st = connection.prepareStatement(sentencia);
            st.setString(1, usuario);
            st.setString(2, password);
            st.setString(3,"false");
            st.executeUpdate();
            System.out.println("Profesor creado correctamente");
            connection.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("El profesor ya existe");
            return false;
        }
    }

    /**
     * Comprueba que exista un usuario en el sistema con usuario y password indicados.
     *
     * @param usuario   nombre de usuario.
     * @param password  password del usuario.
     * @return  (true) si existe, (false) si no existe o los datos son erroneos.
     * @throws RemoteException Si hay un error en la conexion remota.
     */
    @Override
    public boolean compruebaProfesor(String usuario, String password) throws RemoteException {
        try {
            Connection connection = conecta();
            String sentencia = "SELECT COUNT(*) as existe FROM profesor WHERE usuario=? and password=?";
            PreparedStatement st = connection.prepareStatement(sentencia);
            st.setString(1,usuario);
            st.setString(2,password);
            ResultSet rs = st.executeQuery();
            boolean existe = rs.getInt("existe") == 1;
            rs.close();
            connection.close();
            return (existe);
        } catch (Exception e){
            return false;
        }
    }

    /**
     * Dar de baja un profesor de la base de datos.
     * @param usuario Usuario del profesor a dar de baja.
     * @throws RemoteException En caso de que se produzca algun error en la conexion con la base de datos.
     */
    @Override
    public void darDeBajaProgesor(String usuario) throws RemoteException {
        try {
            Connection connection = conecta();
            String sentence = "UPDATE Profesor SET baja = ? WHERE usuario = ?";
            PreparedStatement st = connection.prepareStatement(sentence);

            st.setBoolean(1, true);
            st.setString(2, usuario);
            st.executeQuery();
        }catch (SQLException e){
            System.out.println("El profesor con usuario '" + usuario + "' no existe.");
        }
    }

    /**
     * Se registran las preguntas creadas por el profesor sobre un cuestionario de nombre x.
     *
     * @param pregunta       La pregunta que se quiere registrar
     * @param nombreConjunto El nombre del cuestionario al que pertenece
     * @param usuarioProf    El usuario del profesor al que le corresponden las preguntas
     * @throws RemoteException Si hay algun error en la conexion
     */
    @Override
    public void registraPreguntas(Pregunta pregunta, String nombreConjunto, String usuarioProf) throws RemoteException {
        try {
            Connection connection = conecta();
            String sentencia = "INSERT INTO pregunta VALUES (?, ?, ?, ?, ?)";
            PreparedStatement st = connection.prepareStatement(sentencia);

            st.setString(1, pregunta.getEnunciado().toString());
            st.setDouble(2, pregunta.getTiempo());
            st.setInt(3, pregunta.getPuntos());
            st.setString(4, nombreConjunto);
            st.setString(5, usuarioProf);

            st.executeUpdate();
            System.out.println("Pregunta guardada correctamente.");

            int idPregunta = getLastIdPregunta();
            registraRespuestas(pregunta.getRespuestas(), idPregunta);
            connection.close();
        } catch (SQLException e) {
            System.out.println("A fatal error has ocurred --> registraPreguntas");
        }
    }

    /**
     * Registra las diferentes opciones de respuesta a una pregunta
     *
     * @param respuestas        Listado de opciones
     * @param idPregunta        Identificador de la pregunta a la cual corresponden las opciones
     * @throws RemoteException Si hay algun error en la conexion
     */
    @Override
    public void registraRespuestas(List<Respuesta> respuestas, int idPregunta) throws RemoteException {
        try {
            Connection connection = conecta();
            String sentencia = "INSERT INTO Respuesta VALUES (?, ?, ?)";
            PreparedStatement st = connection.prepareStatement(sentencia);

            for (Respuesta respuesta : respuestas) {
                st.setInt(1, idPregunta);
                st.setString(2, respuesta.getRespuesta());
                st.setBoolean(3, respuesta.isCorrecta());

                st.executeUpdate();
                System.out.println("Respuesta registrada correctamente.");
            }
            connection.close();
        } catch (SQLException e) {
            System.out.println("A fatal error has ocurred --> registraRespuestas");
        }
    }

    /**
     * Se obtienen los cuestionarios creador por un profesor dado.
     *
     * @param usuario El nombre de usuario del pro fesor del cual se quieren obtener sus cuestionarios
     * @return Un mapa en el que se encuentran el nombre del cuestionario y el listado de preguntas asociadas al mismo.
     * @throws RemoteException En el caso de que haya algun error en la conexion con la base de datos
     */
    @Override
    public List<Sesion> getSesionesProfesor(String usuario) {
        List<Sesion> misSesiones = new ArrayList<>();
        Map<String, List<Pregunta>> result = new HashMap<>();
        try {
            getPreguntasProfesor(result, usuario);
            toSesion(misSesiones, result);
        } catch (SQLException e) {
            System.out.println("Ha ocurrido un error al obtener los cuestionarios del profesor");
        }

        return misSesiones;
    }

    /**
     * Metodo que permite obtener un profesor a partir de un usuario
     * @param usuario el nombre de usuario dado para obtener el profesor de la base de datos
     * @return el profesor obtenido de la bbdd o null en el caso de que no se encuentre el profesor
     * @throws RemoteException En el caso de que algo falle.
     */
    @Override
    public Profesor getProfesor(String usuario) throws RemoteException {
        try {
            Connection connection = conecta();
            String sentence = "SELECT * from Profesor WHERE usuario = ?";
            PreparedStatement st = connection.prepareStatement(sentence);

            st.setString(1, usuario);

            ResultSet rs = st.executeQuery();
            Profesor p = new Profesor();

            p.setUsuario(rs.getString("usuario"));
            p.setPassword(rs.getString("password"));
            p.setMisSesiones(getSesionesProfesor(usuario));

            connection.close();
            return p;

        }catch (SQLException e){
            System.out.println("El profesor no existe.");
            return null;
        }
    }

    /**
     * Permite editar una pregunta ya registrada.
     * @param pregunta Los datos de la pregunta modificada.
     * @param usuarioProf El usuario al que pertenece la pregunta.
     * @param nombreCuestionario El cuestionario al que pertenece la pregunta.
     * @throws RemoteException En el caso de que haya algun error en la conexion.
     */
    @Override
    public void editaPregunta(Pregunta pregunta, String usuarioProf, String nombreCuestionario) throws RemoteException {
        try{
            Connection connection = conecta();
            String sentence = "UPDATE Pregunta SET enunciado = ?, tiempo = ?, puntos = ? WHERE usario = ? AND nombreCuestionario = ? AND enunciado = ?";
            PreparedStatement st = connection.prepareStatement(sentence);

            st.setString(1, pregunta.getEnunciado().toString());
            st.setDouble(2, pregunta.getTiempo());
            st.setInt(3, pregunta.getPuntos());
            st.setString(4, usuarioProf);
            st.setString(5, nombreCuestionario);
            st.setString(6, pregunta.getEnunciado().toString());

            st.executeQuery();
            connection.close();
        }catch (SQLException e){
            System.out.println("La pregunta no fue encontrada.");
        }
    }

    /**
     * Permite eliminar una pregunta registrada.
     * @param pregunta Pregunta que quiere eliminar.
     * @param usuarioProf Usuario al que pertenece la pregunta.
     * @param nombreCuestionario Cuestionario al que pertenece la pregunta.
     * @throws RemoteException En el caso de que haya algun error en la conexion con la base de datos.
     */
    @Override
    public void eliminaPregunta(Pregunta pregunta, String usuarioProf, String nombreCuestionario) throws RemoteException {
        try{
            Connection connection = conecta();
            String sentence = "DELETE FROM Pregunta WHERE usario = ? AND nombreCuestionario = ? AND enunciado = ?";
            PreparedStatement st = connection.prepareStatement(sentence);

            st.setString(1, usuarioProf);
            st.setString(2, nombreCuestionario);
            st.setString(3, pregunta.getEnunciado().toString());

            st.executeQuery();
            connection.close();
        }catch (SQLException e){
            System.out.println("La pregunta no fue encontrada.");
        }
    }

    /**
     * Obtener las respuestas de una pregunta dada
     * @param respuestas Resto de respuestas incorrectas de la pregunta
     * @param rs2 Resultado de la ejecucion de la sentencia SQL
     * @return Respuesta correcta que se asigna a la pregunta
     * @throws SQLException En el caso de que haya algun error en la obtencion de los parametros a partir del resultado
     * de la sentencia SQL
     */
    private void getRespuestas(List<Respuesta> respuestas, ResultSet rs2) throws SQLException {
        String respuesta;
        boolean correcta;
        while (rs2.next()) {
            respuesta = rs2.getString("opcion");
            correcta = rs2.getBoolean("correcta");

            respuestas.add(new Respuesta(respuesta, correcta));
            /*
            if (correcta) {
                respuestaCorrecta = respuesta;
            } else {
                respuestas.add(respuesta);
            }
            */
        }
        //return respuestaCorrecta;
    }

    /**
     * Obtener el ultimo id de las preguntas que hay en la base de datos.
     *
     * @return el id de la ultima pregunta anyadida
     */
    private int getLastIdPregunta() {
        try {
            Connection connection = conecta();
            String sentencia = "SELECT TOP 1 idPregunta FROM Preguntas ORDER BY idPregunta DESC";
            PreparedStatement st = connection.prepareStatement(sentencia);

            ResultSet rs = st.executeQuery();
            int idPregunta = rs.getInt("idPregunta");

            connection.close();
            return idPregunta;
        } catch (SQLException e) {
            System.out.println("Ha ocurrido un error al obtener el último id de las preguntas");
            return -1;
        }
    }

    /**
     * Se convierte del mapa obtenido del metodo getPreguntasProfesor a un listado de las sesiones del profesor.
     * @param misSesiones Listado al cual se quieren anyadir el resultado de obtener el mapa de sesiones y preguntas
     * @param result Mapa que contiene el nombre de las sesiones y las preguntas correspondientes a cada sesion
     */
    private void toSesion(List<Sesion> misSesiones, Map<String, List<Pregunta>> result) {
        for (String sesion : result.keySet()){
            Sesion s = new Sesion(sesion);
            s.setListaPreguntas(FXCollections.observableArrayList(result.get(sesion)));

            misSesiones.add(s);
        }
    }

    /**
     * Obtiene el listado de preguntas separadas por el nombre de la sesion a la que pertenecen.
     * @param result Mapa donde se guarda el nombre  de la sesion y las preguntas correspondientes.
     * @param usuario Nombre del profesor del cual se quieren obtener las preguntas.
     * @throws SQLException En el caso de que haya algun error.
     */
    private void getPreguntasProfesor(Map<String, List<Pregunta>> result, String usuario) throws SQLException {
        Connection connection = conecta();
        String sentence = "SELECT * FROM Preguntas WHERE usuario = ? GROUP BY nombreCuestionario";
        PreparedStatement st = connection.prepareStatement(sentence);

        st.setString(1, usuario);

        ResultSet rs = st.executeQuery();
        Pregunta p = new Pregunta();

        String cuestionario;
        int idPregunta;
        //String respuestaCorrecta = "";
        List<Respuesta> respuestas = new ArrayList<>();
        List<Pregunta> listPreguntas;

        sentence = "SELECT opcion, correcta FROM Respuesta WHERE idPregunta = ?";
        st = connection.prepareStatement(sentence);

        while (rs.next()) {
            idPregunta = rs.getInt("idPregunta");
            cuestionario = rs.getString("nombreCuestionario");


            st.setInt(1, idPregunta);
            ResultSet rs2 = st.executeQuery();

            //respuestaCorrecta = getRespuestas(respuestaCorrecta, respuestas, rs2);
            getRespuestas(respuestas,rs2);

            p.setEnunciado(rs.getString("enunciado"));
            p.setRespuestas(respuestas);
            p.setTiempo(rs.getDouble("tiempo"));
            p.setPuntos(rs.getInt("puntos"));

            result.computeIfAbsent(cuestionario, k -> new ArrayList<>());
            listPreguntas = result.get(cuestionario);
            listPreguntas.add(p);
            result.put(cuestionario, listPreguntas);
        }
        connection.close();
    }
}
