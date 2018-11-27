package com.election;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Servidor extends Thread {
    private ServerSocket socketServer;
    private Processo p;

    public Servidor(int porta, Processo p) {
        try {
            this.socketServer = new ServerSocket(porta);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.p = p;
    }

    public void listen() {
        Socket socketClient;
        Canal canal;
        while (true){
            try {
                socketClient = socketServer.accept(); // Bloqueia até receber conexão
                canal = new Canal(socketClient, this.p);
                canal.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        listen();
    }

}
