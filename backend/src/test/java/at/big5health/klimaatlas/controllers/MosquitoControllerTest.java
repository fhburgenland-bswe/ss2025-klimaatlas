package at.big5health.klimaatlas.controllers;

import at.big5health.klimaatlas.dtos.MosquitoOccurrenceDTO;
import at.big5health.klimaatlas.services.MosquitoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(MosquitoController.class)
class MosquitoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MosquitoService mosquitoService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllMosquitoOccurrences_shouldReturn200WithData() throws Exception {
        List<MosquitoOccurrenceDTO> mockList = List.of(
                new MosquitoOccurrenceDTO(48.2082, 16.3738, "Aedes albopictus", "2025-05-20"),
                new MosquitoOccurrenceDTO(47.0707, 15.4395, "Culex pipiens", "2025-05-18")
        );
        given(mosquitoService.getOccurrences()).willReturn(mockList);

        mockMvc.perform(get("/mosquitoes")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].latitude").value(48.2082))
                .andExpect(jsonPath("$[0].longitude").value(16.3738))
                .andExpect(jsonPath("$[0].species").value("Aedes albopictus"))
                .andExpect(jsonPath("$[0].eventDate").value("2025-05-20"));
    }
}