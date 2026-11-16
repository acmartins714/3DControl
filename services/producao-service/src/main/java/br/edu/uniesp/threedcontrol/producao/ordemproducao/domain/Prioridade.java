package br.edu.uniesp.threedcontrol.producao.ordemproducao.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Prioridade derivada da proximidade do prazo de entrega pactuado. */
public enum Prioridade {
    URGENTE(4), ALTA(3), NORMAL(2), BAIXA(1);

    private final int peso;

    Prioridade(int peso) {
        this.peso = peso;
    }

    public int peso() {
        return peso;
    }

    public static Prioridade para(LocalDate prazoEntrega, LocalDate hoje) {
        long dias = ChronoUnit.DAYS.between(hoje, prazoEntrega);
        if (dias <= 1) return URGENTE;
        if (dias <= 3) return ALTA;
        if (dias <= 7) return NORMAL;
        return BAIXA;
    }
}
