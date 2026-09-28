package br.com.senac.tarefas.dto;

import br.com.senac.tarefas.model.StatusTarefa;
import br.com.senac.tarefas.model.Tarefa;
import java.time.LocalDateTime;

public record TarefaResponse(
        Long id,
        String titulo,
        String descricao,
        StatusTarefa status,
        LocalDateTime dataCriacao,
        LocalDateTime dataConclusao) {

    public static TarefaResponse from(Tarefa t) {
        return new TarefaResponse(t.getId(), t.getTitulo(), t.getDescricao(),
                t.getStatus(), t.getDataCriacao(), t.getDataConclusao());
    }
}
