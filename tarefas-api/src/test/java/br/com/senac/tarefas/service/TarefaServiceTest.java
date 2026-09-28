package br.com.senac.tarefas.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import br.com.senac.tarefas.dto.TarefaRequest;
import br.com.senac.tarefas.dto.TarefaResponse;
import br.com.senac.tarefas.exception.RecursoNaoEncontradoException;
import br.com.senac.tarefas.exception.RegraDeNegocioException;
import br.com.senac.tarefas.model.StatusTarefa;
import br.com.senac.tarefas.model.Tarefa;
import br.com.senac.tarefas.repository.TarefaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TarefaServiceTest {

    @Mock
    private TarefaRepository repository;

    @InjectMocks
    private TarefaService service;

    private Tarefa tarefa(Long id, String titulo, StatusTarefa status) {
        Tarefa t = new Tarefa();
        t.setId(id);
        t.setTitulo(titulo);
        t.setDescricao("desc");
        t.setStatus(status);
        t.setDataCriacao(LocalDateTime.now());
        return t;
    }

    // ---------- CRIAR ----------

    @Test
    @DisplayName("Criar: caminho feliz assume PENDENTE quando status não informado")
    void criarComSucesso() {
        TarefaRequest req = new TarefaRequest("Estudar Mockito", "desc", null, null);
        when(repository.existsByTituloIgnoreCaseAndStatusNot("Estudar Mockito", StatusTarefa.CONCLUIDA)).thenReturn(false);
        when(repository.save(any(Tarefa.class))).thenAnswer(inv -> {
            Tarefa t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TarefaResponse resp = service.criar(req);

        assertEquals(1L, resp.id());
        assertEquals("Estudar Mockito", resp.titulo());
        assertEquals(StatusTarefa.PENDENTE, resp.status());
        verify(repository, times(1)).save(any(Tarefa.class));
    }

    @Test
    @DisplayName("Criar: título duplicado em tarefa ativa lança exceção")
    void criarTituloDuplicado() {
        TarefaRequest req = new TarefaRequest("Estudar Mockito", null, null, null);
        when(repository.existsByTituloIgnoreCaseAndStatusNot("Estudar Mockito", StatusTarefa.CONCLUIDA)).thenReturn(true);

        RegraDeNegocioException ex = assertThrows(RegraDeNegocioException.class, () -> service.criar(req));

        assertTrue(ex.getMessage().contains("Estudar Mockito"));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Criar: não permite criar tarefa já CONCLUIDA")
    void criarJaConcluida() {
        TarefaRequest req = new TarefaRequest("Tarefa pronta", null, StatusTarefa.CONCLUIDA, LocalDateTime.now());

        assertThrows(RegraDeNegocioException.class, () -> service.criar(req));

        verify(repository, never()).save(any());
    }

    // ---------- CONSULTAR ----------

    @Test
    @DisplayName("Listar: retorna todas as tarefas")
    void listar() {
        when(repository.findAll()).thenReturn(List.of(
                tarefa(1L, "Primeira tarefa", StatusTarefa.PENDENTE),
                tarefa(2L, "Segunda tarefa", StatusTarefa.EM_ANDAMENTO)));

        List<TarefaResponse> lista = service.listar();

        assertEquals(2, lista.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Buscar por ID: caminho feliz")
    void buscarPorId() {
        when(repository.findById(1L)).thenReturn(Optional.of(tarefa(1L, "Primeira tarefa", StatusTarefa.PENDENTE)));

        TarefaResponse resp = service.buscarPorId(1L);

        assertEquals("Primeira tarefa", resp.titulo());
        verify(repository).findById(1L);
    }

    @Test
    @DisplayName("Buscar por ID inexistente lança RecursoNaoEncontradoException")
    void buscarPorIdInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.buscarPorId(99L));
    }

    // ---------- ATUALIZAR ----------

    @Test
    @DisplayName("Atualizar: PENDENTE -> EM_ANDAMENTO com sucesso")
    void atualizarComSucesso() {
        Tarefa existente = tarefa(1L, "Tarefa original", StatusTarefa.PENDENTE);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.existsByTituloIgnoreCaseAndStatusNotAndIdNot(anyString(), any(), anyLong())).thenReturn(false);
        when(repository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));

        TarefaResponse resp = service.atualizar(1L,
                new TarefaRequest("Tarefa alterada", "nova desc", StatusTarefa.EM_ANDAMENTO, null));

        assertEquals("Tarefa alterada", resp.titulo());
        assertEquals(StatusTarefa.EM_ANDAMENTO, resp.status());
        verify(repository, times(1)).save(existente);
    }

    @Test
    @DisplayName("Atualizar: EM_ANDAMENTO -> CONCLUIDA com data de conclusão")
    void atualizarParaConcluida() {
        Tarefa existente = tarefa(1L, "Tarefa original", StatusTarefa.EM_ANDAMENTO);
        LocalDateTime fim = LocalDateTime.now();
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));

        TarefaResponse resp = service.atualizar(1L,
                new TarefaRequest("Tarefa original", null, StatusTarefa.CONCLUIDA, fim));

        assertEquals(StatusTarefa.CONCLUIDA, resp.status());
        assertEquals(fim, resp.dataConclusao());
    }

    @Test
    @DisplayName("Atualizar: PENDENTE -> CONCLUIDA direto é proibido")
    void atualizarTransicaoInvalida() {
        Tarefa existente = tarefa(1L, "Tarefa original", StatusTarefa.PENDENTE);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        TarefaRequest req = new TarefaRequest("Tarefa original", null, StatusTarefa.CONCLUIDA, LocalDateTime.now());
        assertThrows(RegraDeNegocioException.class, () -> service.atualizar(1L, req));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Atualizar: CONCLUIDA sem dataConclusao é rejeitado")
    void atualizarConcluidaSemData() {
        Tarefa existente = tarefa(1L, "Tarefa original", StatusTarefa.EM_ANDAMENTO);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        TarefaRequest req = new TarefaRequest("Tarefa original", null, StatusTarefa.CONCLUIDA, null);
        assertThrows(RegraDeNegocioException.class, () -> service.atualizar(1L, req));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Atualizar: novo título já usado por outra tarefa ativa é rejeitado")
    void atualizarTituloDuplicado() {
        Tarefa existente = tarefa(1L, "Tarefa original", StatusTarefa.PENDENTE);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.existsByTituloIgnoreCaseAndStatusNotAndIdNot("Outro titulo", StatusTarefa.CONCLUIDA, 1L)).thenReturn(true);

        TarefaRequest req = new TarefaRequest("Outro titulo", null, null, null);
        assertThrows(RegraDeNegocioException.class, () -> service.atualizar(1L, req));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Atualizar: ID inexistente lança exceção")
    void atualizarInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        TarefaRequest req = new TarefaRequest("Qualquer titulo", null, null, null);
        assertThrows(RecursoNaoEncontradoException.class, () -> service.atualizar(99L, req));
    }

    // ---------- EXCLUIR ----------

    @Test
    @DisplayName("Excluir: tarefa não concluída é removida")
    void excluirComSucesso() {
        Tarefa existente = tarefa(1L, "Tarefa original", StatusTarefa.PENDENTE);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        service.excluir(1L);

        verify(repository, times(1)).delete(existente);
    }

    @Test
    @DisplayName("Excluir: tarefa CONCLUIDA é rejeitada")
    void excluirConcluida() {
        Tarefa existente = tarefa(1L, "Tarefa original", StatusTarefa.CONCLUIDA);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        assertThrows(RegraDeNegocioException.class, () -> service.excluir(1L));

        verify(repository, never()).delete(any(Tarefa.class));
    }

    @Test
    @DisplayName("Excluir: ID inexistente lança exceção")
    void excluirInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.excluir(99L));
    }
}
