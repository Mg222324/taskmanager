package br.com.senac.tarefas.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErroResponse(int status, String erro, String mensagem, List<String> detalhes, LocalDateTime timestamp) {
    public ErroResponse(int status, String erro, String mensagem, List<String> detalhes) {
        this(status, erro, mensagem, detalhes, LocalDateTime.now());
    }
}
