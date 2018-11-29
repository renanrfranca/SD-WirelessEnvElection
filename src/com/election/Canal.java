package com.election;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class Canal extends Thread {
    private Socket socket;
    private Processo processo;

    public Canal(Socket socket, Processo processo){
        this.socket = socket;
        this.processo = processo;
    }

    public void extraiMensagem(){
        ObjectInputStream entrada = null;
        try {
            entrada = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //observa continuamente o link entre o servidor e os clientes, com intuito de
        //captar mensagens
        try {
            while (true) {
                Mensagem mensagem = (Mensagem)entrada.readObject();

                processo.recebeMensagem(mensagem);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        extraiMensagem();
    }
}
