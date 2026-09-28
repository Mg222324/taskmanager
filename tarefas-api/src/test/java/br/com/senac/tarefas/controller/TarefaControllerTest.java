package br.com.senac.tarefas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.senac.tarefas.dto.TarefaResponse;
import br.com.senac.tarefas.exception.GlobalExceptionHandler;
import br.com.senac.tarefas.exception.RecursoNaoEncontradoException;
import br.com.senac.tarefas.exception.RegraDeNegocioException;
import br.com.senac.tarefas.model.StatusTarefa;
import br.com.senac.tarefas.service.TarefaService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TarefaController.class)
@Import(GlobalExceptionHandler.class)
class TarefaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TarefaService service;

    private TarefaResponse resposta() {
        return new TarefaResponse(1L, "Estudar Mockito", "desc", StatusTarefa.PENDENTE, LocalDateTime.now(), null);
    }

    @Test
    void postValidoRetorna201() throws Exception {
        when(service.criar(any())).thenReturn(resposta());

        mockMvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Estudar Mockito\",\"descricao\":\"desc\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titulo").value("Estudar Mockito"))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void postComTituloCurtoRetorna400() throws Exception {
        mockMvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detalhes[0]").exists());
    }

    @Test
    void getListaRetorna200() throws Exception {
        when(service.listar()).thenReturn(List.of(resposta()));

        mockMvc.perform(get("/api/tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Estudar Mockito"));
    }

    @Test
    void getPorIdRetorna200() throws Exception {
        when(service.buscarPorId(1L)).thenReturn(resposta());

        mockMvc.perform(get("/api/tarefas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getPorIdInexistenteRetorna404() throws Exception {
        when(service.buscarPorId(99L)).thenThrow(new RecursoNaoEncontradoException("Tarefa não encontrada com id: 99"));

        mockMvc.perform(get("/api/tarefas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Tarefa não encontrada com id: 99"));
    }

    @Test
    void putComRegraViolada400() throws Exception {
        when(service.atualizar(any(), any())).thenThrow(new RegraDeNegocioException("Transição inválida"));

        mockMvc.perform(put("/api/tarefas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Estudar Mockito\",\"status\":\"CONCLUIDA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Transição inválida"));
    }

    @Test
    void deleteRetorna204() throws Exception {
        mockMvc.perform(delete("/api/tarefas/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteConcluidaRetorna400() throws Exception {
        doThrow(new RegraDeNegocioException("Não é possível excluir uma tarefa já concluída"))
                .when(service).excluir(1L);

        mockMvc.perform(delete("/api/tarefas/1"))
                .andExpect(status().isBadRequest());
    }
}
