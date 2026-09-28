package br.com.senac.tarefas.repository;

import br.com.senac.tarefas.model.StatusTarefa;
import br.com.senac.tarefas.model.Tarefa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TarefaRepository extends JpaRepository<Tarefa, Long> {

    /** Tarefa "ativa" = qualquer status diferente do informado (CONCLUIDA). */
    boolean existsByTituloIgnoreCaseAndStatusNot(String titulo, StatusTarefa status);

    boolean existsByTituloIgnoreCaseAndStatusNotAndIdNot(String titulo, StatusTarefa status, Long id);
}
