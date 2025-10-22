package com.example.hack1.SecurityConfig;

// Importar los matchers estáticos de MockMvc
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "CENTRAL")
    void whenGetUserList_asCentral_shouldReturnOk() throws Exception {
        // WHEN: Hacemos un GET a /users
        mockMvc.perform(get("/users"))
                // THEN: Esperamos que nos deje pasar (200 OK)
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "BRANCH") // 5. "Fingir" ser un usuario de sucursal
    void whenGetUserList_asBranch_shouldReturnForbidden() throws Exception {
        // WHEN: Hacemos un GET a /users
        mockMvc.perform(get("/users"))
                // THEN: Esperamos que nos bloquee (403 Forbidden)
                .andExpect(status().isForbidden());
    }

    @Test
    void whenGetUserList_asAnonymous_shouldReturnUnauthorized() throws Exception {
        // WHEN: Hacemos un GET a /users (sin @WithMockUser)
        mockMvc.perform(get("/users"))
                // THEN: Esperamos que nos pida login (401 Unauthorized)
                .andExpect(status().isUnauthorized());
    }

    // ... puedes hacer lo mismo para DELETE /users/{id}, etc.

    @Test
    @WithMockUser(roles = "BRANCH", username = "miraflores.user")
    void whenGetOwnSale_asBranch_shouldReturnOk() throws Exception {
        // Este test es más complejo porque requiere que el
        // servicio/repositorio esté mockeado para devolver una venta
        // que "pertenezca" a "miraflores.user".

        // mockMvc.perform(get("/sales/s_123"))
        //         .andExpect(status().isOk());
    }
}
