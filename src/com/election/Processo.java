package com.election;

import java.util.*;
import java.net.*;
import java.io.*;

public class Processo extends Thread {
    private final String ENDERECO = "localhost";
    private int pid;
    private int capacidade;
    private int pidLeader;
    private ArrayList<No> listaNos;
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

        this.listaNos = new ArrayList<>();
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
            No no = new No(socket);
            listaNos.add(no);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startElection(){
        this.electionStarterPid = this.pid;
        this.pidPai = -1;
        this.numAcks = 0;

        System.out.println("iniciando eleição com id " + this.electionStarterPid);
        Mensagem msgSend = new Mensagem("--election", this.pid, this.electionStarterPid);
        // envia eleiçao para todos os vizinhos
        for (No no : listaNos)
            sendMessage(no, msgSend);
    }

    public void recebeMensagem(Mensagem m){
        try {
            Thread.sleep((pid - 9000) * 250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (m.texto.equals("--ack")){
            this.recebeAck(m);
            return;
        }

        if (m.texto.equals("--election")){
            System.out.println(this.pid + " recebeu solicitacao de [ELEICAO] <" + m.electionStarterPid + "> de <"+m.senderPid+">");
            // Se não já estabeleceu um pai de uma eleição corrente
            if (electionStarterPid == -1){
                this.handleElection(m);
                return;
            }

            // Verifica se a msg se refere a uma eleição com mais prioridade
            if (m.electionStarterPid > this.electionStarterPid){
                // Cancela eleição atual e lida com a nova
                // se tem um pai, envia nak pro pai
                System.out.println("Eleicao antiga cancelada por prioridade. " +
                        "Recomecando nova eleicao...");
                sendNak(pidPai, this.electionStarterPid);
                // reinicia variaveis de eleição
                this.endElection();
                // lida com a nova eleição
                this.handleElection(m);
                return;
            }

            // Se chegou até aqui ou é relativa a mesma eleição (ACK)
            // ou a uma eleição com prioridade inferior (NAK)
            if (m.electionStarterPid == this.electionStarterPid){
                sendAck(m.senderPid);
            } else {
                System.out.println("Eleicao com prioridade inferior ignorada!");
                sendNak(m.senderPid, m.electionStarterPid);
            }
            return;
        }

        if (m.texto.equals("--leader")){
            // gambiarra (electionStarter pid da msg carrega o valor do lider novo)
            System.out.println("recebeu [LEADER] de " + m.senderPid + ". Lider: <"+m.electionStarterPid + ">." );
            if (m.electionStarterPid != this.pidLeader){
                this.pidLeader = m.electionStarterPid;
                this.informaLider();
            }
            return;
        }

        if (m.texto.equals("--nak")){
            System.out.println(this.pid + "recebeu [NAK] de <"+m.senderPid + "> referente a eleição de <" + m.electionStarterPid + ">");
            // Se o nak se refere a eleição vigente, a cancela, se não ignora
            if (m.electionStarterPid == this.electionStarterPid){
                System.out.println("cancelando eleição " + this.electionStarterPid);
                // se tem um pai, envia nak pro pai
                if (pidPai >= 0)
                    sendNak(pidPai, this.electionStarterPid);
                // reinicia variaveis de eleição
                this.endElection();
            }
            return;
        }
    }

    private void handleElection(Mensagem m){
        this.electionStarterPid = m.electionStarterPid;
        System.out.println(this.pid + " atribuiu <"+ m.senderPid + "> como PAI");
        this.pidPai = m.senderPid;
        this.numAcks = 0;

        //Pai é o unico vizinho, termina eleicao
        if(listaNos.size() == 1) {
            this.respondeEleicao();
            this.endElection();
            return;
        }

        Mensagem msgSend = new Mensagem("--election", this.pid, this.electionStarterPid);


        // envia eleiçao para todos os vizinhos tirando o pai
        for (No no : listaNos) {
            if (no.getPort() != this.pidPai){
                System.out.println("Encaminhando eleição para " + no.getPort());
                sendMessage(no, msgSend);
            }
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
        System.out.println("recebeu [ACK] de <" + m.senderPid + ">" );

        if (this.electionStarterPid < 0) {
            System.out.println("Ack ignorado por não haver eleição vigente");
            return;
        }
        numAcks++;

        // Se msg veio sem info de capacidade, é -1, portanto não entra
        if (this.bestKnownCapacity < m.bestCapacity){
            System.out.println("Capacidade do ack maior que a conhecida");
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
        No no = getNo(pidPai);
        System.out.println("Retornando para o pai " + pidPai +
                ":\n BestPID: " + this.bestKnownCapacityPid + "BestCapacity: " + this.bestKnownCapacity);
        Mensagem m = new Mensagem("--ack", this.pid, this.electionStarterPid, this.bestKnownCapacityPid, this.bestKnownCapacity);
        sendMessage(no, m);
    }

    private void informaLider(){
        // gambiarra (electionStarter pid da msg carrega o valor do lider novo)
        Mensagem m = new Mensagem("--leader", this.pid, this.pidLeader);

        // envia eleiçao para todos os vizinhos
        for (No no : listaNos)
            sendMessage(no, m);
    }

    private void sendAck(int porta){
        No no = getNo(porta);
        Mensagem m = new Mensagem("--ack", this.pid);
        System.out.println("Enviando ack para <" + no.getPort() + "> referente a eleição <" + m.electionStarterPid + ">");
        sendMessage(no, m);
    }

    private void sendNak(int porta, int electionStarterPid){
        No no = getNo(porta);
        Mensagem m = new Mensagem("--nak", this.pid, electionStarterPid);
        System.out.println("Enviando nak para <" + no.getPort() + "> referente a eleição <" + m.electionStarterPid + ">");
        sendMessage(no, m);
    }

    private void sendMessage(No no, Mensagem m){
        try {
            no.getOos().writeObject(m);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private No getNo(int porta){
        for (No no : listaNos) {
            if (porta == no.getPort())
                return no;
        }
        return null;
    }

    private void startServer(){
        this.s.start();
    }

    public int getNumNos(){
        return this.listaNos.size();
    }

    // Método sobrecarregado de Thread,
    // Executado automaticamente pela JVM em Thread.start()
    @Override
    public void run(){
        startServer();
    }
}
