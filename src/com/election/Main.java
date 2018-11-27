package com.election;

import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        // Verifica se a porta foi informada e armazena na variavel
        if (args.length != 2){
            System.out.println("O programa necessita de 2 argumentos (porta do processo e capacidade)");
            return;
        }
        int porta = Integer.parseInt(args[0]);
        int capacidade = Integer.parseInt(args[1]);

        // PID = Porta
        Processo p = new Processo(porta, capacidade);
        p.start();

        // Menu
        Scanner scanner = new Scanner(System.in);
        int operacao;
        do {
            System.out.println("Informe a operação desejada: 1 - Conectar a porta, 2 - iniciar eleição, 0 - Sair");
            operacao = Integer.parseInt(scanner.nextLine());
            switch(operacao) {
                case 1:
                    p.connect(promptPorta(scanner));
                    break;
                case 2:
                    p.startElection();
                    break;
            }
        } while (operacao != 0);
    }

    private static int promptPorta(Scanner s){
        System.out.print("Informe a porta: ");
        return Integer.parseInt(s.nextLine());
    }
}
