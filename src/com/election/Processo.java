package com.election;

import java.util.*;
import java.net.*;
import java.io.*;

public class Processo extends Thread {
    private final String ENDERECO = "localhost";
    private int pid;
    private int capacidade;
    private int pidLeader;
    private ArrayList<Socket> listaSockets;
    private Servidor s;

    // Variáveis relativas a eleição
    private int electionStarterPid;
    private int pidPai;
    private int numAcks;
    private int bestKnownCapacityPid;
    private int bestKnownCapacity;

    public Processo(int porta, int capacidade) {
        this.pid = porta;
        this.capacidade = capacidade;
        this.pidLeader = -1;

        listaSockets = new ArrayList<>();
        this.s = new Servidor(porta, this);

        this.electionStarterPid = -1;
        this.pidPai = -1;
        this.numAcks = 0;
        this.bestKnownCapacityPid = this.pid;
        this.bestKnownCapacity = this.capacidade;
    }

    public void connect(int porta){
        Socket socket;
        try {
            socket = new Socket(ENDERECO, porta);
            listaSockets.add(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startElection(){
        this.electionStarterPid = this.pid;
        this.pidPai = -1;
        this.numAcks = 0;

        Mensagem msgSend = new Mensagem("--election", this.pid, this.electionStarterPid);

        // envia eleiçao para todos os vizinhos
        for (Socket s : listaSockets)
            sendMessage(s, msgSend);
    }

    public void recebeMensagem(Mensagem m){
        if (m.texto.equals("--ack")){
            this.recebeAck(m);
            return;
        }

        if (m.texto.equals("--election")){
            System.out.println(this.pid + " recebeu solicitacao de [ELEICAO] de <"+m.senderPid+">");
            // Se já não estabeleceu um pai de uma eleição corrente
            if (electionStarterPid == -1){
                System.out.println(this.pid + " atribuiu <"+ m.senderPid + "> como PAI");
                this.handleElection(m);
                return;
            }

            // Verifica se a msg se refere a uma eleição com mais prioridade
            if (m.electionStarterPid > this.electionStarterPid){
                // Cancela eleição atual e lida com a nova
                // se tem um pai, envia nak pro pai
                System.out.println("Eleicao antiga cancelada por prioridade. " +
                        "Recomecando nova eleicao...");
                sendNak(pidPai);
                // reinicia variaveis de eleição
                this.endElection();
                // lida com a nova eleição
                this.handleElection(m);
                return;
            }

            // Se chegou até aqui ou é relativa a mesma eleição (ACK)
            // ou a uma eleição com prioridade inferior (NAK)
            if (m.electionStarterPid == this.electionStarterPid)
                sendAck(m.senderPid);
            else
                System.out.println("Eleicao com prioridade inferior ignorada!");
                sendNak(m.senderPid);

            return;
        }

        if (m.texto.equals("--leader")){
            // gambiarra (electionStarter pid da msg carrega o valor do lider novo)
            System.out.println(this.pid + " recebeu [LEADER]. Lider: <"+m.electionStarterPid + ">." );
            if (m.electionStarterPid != this.pidLeader){
                this.pidLeader = m.electionStarterPid;
                this.informaLider();
            }
        }

        if (m.texto.equals("--nak")){
            // se tem um pai, envia nak pro pai
            System.out.println(this.pid + "recebeu [NAK] de <"+m.senderPid + ">");
            if (pidPai >= 0)
                sendNak(pidPai);
            // reinicia variaveis de eleição
            this.endElection();
            return;
        }
    }

    private void handleElection(Mensagem m){
        this.electionStarterPid = m.electionStarterPid;
        this.pidPai = m.senderPid;
        this.numAcks = 0;

        //Pai é o unico vizinho, termina eleicao
        if(listaSockets.size() == 1) {
            this.respondeEleicao();
            this.endElection();
            return;
        }

        Mensagem msgSend = new Mensagem("--election", this.pid, this.electionStarterPid);


        // envia eleiçao para todos os vizinhos tirando o pai
        for (Socket s : listaSockets) {
            if (s.getPort() != this.pidPai)
                sendMessage(s, msgSend);
        }
    }
    
    private void endElection(){
        // reinicia variaveis de eleição
        this.electionStarterPid = -1;
        this.pidPai = -1;
        this.numAcks = 0;
        this.bestKnownCapacityPid = this.pid;
        this.bestKnownCapacity = this.capacidade;        
    }

    private void recebeAck(Mensagem m){
        // se não há eleição corrente, não faz nada
        System.out.println(this.pid + " recebeu [ACK] de <" + m.senderPid + ">" );

        if (this.electionStarterPid < 0) {
            return;
        }
        numAcks++;

        // Se msg veio sem info de capacidade, é -1, portanto não entra
        if (this.bestKnownCapacity < m.bestCapacity){
            this.bestKnownCapacityPid = m.bestPid;
            this.bestKnownCapacity = m.bestCapacity;
        }

        int acksEsperados;
        // Se não tem pai (é fonte)
        if (this.pidPai < 0)
            acksEsperados = this.getNumNos();
        else
            acksEsperados = this.getNumNos() - 1;

        // Se receber todos os acks que espera
        if (numAcks == acksEsperados){
            this.respondeEleicao();
            this.endElection();
        }
    }

    private void respondeEleicao(){
        // Se ele for o processo foi quem iniciou a eleição, define e informa o líder
        if (pidPai < 0){
            System.out.println("NOVO LIDER DEFINIDO: <" + bestKnownCapacityPid+">");
            this.pidLeader = bestKnownCapacityPid;
            this.informaLider();
            return;
        }

        // envia ack pro pai contendo a melhor capacidade encontrada
        Socket s = getSocket(pidPai);
        Mensagem m = new Mensagem("--ack", this.pid, this.electionStarterPid, this.bestKnownCapacityPid, this.bestKnownCapacity);
        sendMessage(s, m);
    }

    private void informaLider(){
        // gambiarra (electionStarter pid da msg carrega o valor do lider novo)
        Mensagem m = new Mensagem("--leader", this.pid, this.pidLeader);

        // envia eleiçao para todos os vizinhos
        for (Socket s : listaSockets)
            sendMessage(s, m);
    }

    private void sendAck(int porta){
        Socket s = getSocket(porta);
        Mensagem m = new Mensagem("--nak", this.pid);
        sendMessage(s, m);
    }

    private void sendNak(int porta){
        Socket s = getSocket(porta);
        Mensagem m = new Mensagem("--nak", this.pid);
        sendMessage(s, m);
    }

    private void sendMessage(Socket s, Mensagem m){
        ObjectOutputStream out;
        try {
            out = new ObjectOutputStream(s.getOutputStream());
            out.writeObject(m);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private Socket getSocket(int porta){
        for (Socket s : listaSockets) {
            if (porta == s.getPort())
                return s;
        }
        return null;
    }

    private void startServer(){
        this.s.start();
    }

    public int getNumNos(){
        return this.listaSockets.size();
    }

    // Método sobrecarregado de Thread,
    // Executado automaticamente pela JVM em Thread.start()
    @Override
    public void run(){
        startServer();
    }
}
