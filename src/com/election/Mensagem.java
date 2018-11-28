package com.election;

import java.io.Serializable;

public class Mensagem implements Serializable {
    public String texto;
    public int senderPid;
    public int electionStarterPid;
    public int bestPid;
    public int bestCapacity;

    public Mensagem(String texto, int senderPid) {
        this(texto, senderPid, -1, -1, -1);
    }

    public Mensagem(String texto, int senderPid, int electionStarterPid) {
        this(texto, senderPid, electionStarterPid, -1, -1);
    }

    public Mensagem(String texto, int senderPid, int electionStarterPid, int bestPid, int bestCapacity) {
        this.texto = texto;
        this.senderPid = senderPid;
        this.electionStarterPid = electionStarterPid;
        this.bestPid = bestPid;
        this.bestCapacity = bestCapacity;
    }

    @Override
    public String toString() {
        return "Mensagem{" +
                "texto='" + texto + '\'' +
                ", senderPid=" + senderPid +
                ", electionStarterPid=" + electionStarterPid +
                ", bestPid=" + bestPid +
                ", bestCapacity=" + bestCapacity +
                '}';
    }
}
