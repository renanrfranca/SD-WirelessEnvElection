package com.election;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class No {
    private Socket s;
    private int port;
    private ObjectOutputStream oos;

    public No(Socket s) {
        this.s = s;
        this.port = s.getPort();
        try {
            this.oos = new ObjectOutputStream(s.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return port;
    }

    public ObjectOutputStream getOos() {
        return oos;
    }
}
