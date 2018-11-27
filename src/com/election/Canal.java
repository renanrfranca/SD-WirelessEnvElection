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
        ObjectInputStream entrada;
        //observa continuamente o link entre o servidor e os clientes, com intuito de
        //captar mensagens
        try {
            while (true) {
                entrada = new ObjectInputStream(socket.getInputStream());
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
