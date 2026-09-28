package br.com.senac.tarefas.service;

import br.com.senac.tarefas.dto.TarefaRequest;
import br.com.senac.tarefas.dto.TarefaResponse;
import br.com.senac.tarefas.exception.RecursoNaoEncontradoException;
import br.com.senac.tarefas.exception.RegraDeNegocioException;
import br.com.senac.tarefas.model.StatusTarefa;
import br.com.senac.tarefas.model.Tarefa;
import br.com.senac.tarefas.repository.TarefaRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TarefaService {

    private final TarefaRepository repository;

    public TarefaService(TarefaRepository repository) {
        this.repository = repository;
    }

    public TarefaResponse criar(TarefaRequest req) {
        StatusTarefa status = req.status() != null ? req.status() : StatusTarefa.PENDENTE;
        if (status == StatusTarefa.CONCLUIDA) {
            throw new RegraDeNegocioException("Uma tarefa não pode ser criada já concluída");
        }
        String titulo = req.titulo().trim();
        if (repository.existsByTituloIgnoreCaseAndStatusNot(titulo, StatusTarefa.CONCLUIDA)) {
            throw new RegraDeNegocioException("Já existe uma tarefa ativa com o título: " + titulo);
        }

        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo(titulo);
        tarefa.setDescricao(req.descricao());
        tarefa.setStatus(status);
        return TarefaResponse.from(repository.save(tarefa));
    }

    public List<TarefaResponse> listar() {
        return repository.findAll().stream().map(TarefaResponse::from).toList();
    }

    public TarefaResponse buscarPorId(Long id) {
        return TarefaResponse.from(obter(id));
    }

    public TarefaResponse atualizar(Long id, TarefaRequest req) {
        Tarefa tarefa = obter(id);
        String titulo = req.titulo().trim();

        if (!titulo.equalsIgnoreCase(tarefa.getTitulo())
                && repository.existsByTituloIgnoreCaseAndStatusNotAndIdNot(titulo, StatusTarefa.CONCLUIDA, id)) {
            throw new RegraDeNegocioException("Já existe uma tarefa ativa com o título: " + titulo);
        }

        StatusTarefa novoStatus = req.status() != null ? req.status() : tarefa.getStatus();
        validarTransicao(tarefa.getStatus(), novoStatus);

        if (novoStatus == StatusTarefa.CONCLUIDA) {
            if (req.dataConclusao() == null) {
                throw new RegraDeNegocioException("A data de conclusão é obrigatória para tarefas CONCLUIDA");
            }
            tarefa.setDataConclusao(req.dataConclusao());
        } else {
            tarefa.setDataConclusao(null);
        }

        tarefa.setTitulo(titulo);
        tarefa.setDescricao(req.descricao());
        tarefa.setStatus(novoStatus);
        return TarefaResponse.from(repository.save(tarefa));
    }

    public void excluir(Long id) {
        Tarefa tarefa = obter(id);
        if (tarefa.getStatus() == StatusTarefa.CONCLUIDA) {
            throw new RegraDeNegocioException("Não é possível excluir uma tarefa já concluída");
        }
        repository.delete(tarefa);
    }

    private Tarefa obter(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tarefa não encontrada com id: " + id));
    }

    private void validarTransicao(StatusTarefa atual, StatusTarefa novo) {
        if (atual == StatusTarefa.PENDENTE && novo == StatusTarefa.CONCLUIDA) {
            throw new RegraDeNegocioException("Não é permitido passar de PENDENTE direto para CONCLUIDA; use EM_ANDAMENTO");
        }
    }
}
