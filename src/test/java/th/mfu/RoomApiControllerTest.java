package th.mfu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import th.mfu.domain.Room;

// Calls the REST API the way Postman would: JSON body + Basic Auth with the admin account
@SpringBootTest(properties = "dormhub.gemini.api-key=")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RoomApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RoomRepository roomRepo;

    @Autowired
    private TenantRepository tenantRepo;

    private static RequestPostProcessor admin() {
        return httpBasic("sjdadmin@gmail.com", "dormhub123");
    }

    @Test
    public void testApiNeedsAdmin() throws Exception {
        mvc.perform(get("/api/rooms").accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/rooms").with(httpBasic("somchai@example.com", "tenant123")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetAndSearchRooms() throws Exception {
        mvc.perform(get("/api/rooms").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomNumber").exists());
        mvc.perform(get("/api/rooms").param("q", "A101").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].roomNumber").value("A101"));
        mvc.perform(get("/api/rooms/99999").with(admin())).andExpect(status().isNotFound());
    }

    @Test
    public void testCreateUpdateDeleteRoom() throws Exception {
        mvc.perform(post("/api/rooms").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"roomNumber\":\"Z901\",\"building\":\"Z\",\"floor\":9,\"monthlyRent\":3500,\"capacity\":1}"))
                .andExpect(status().isCreated());
        Room room = roomRepo.findByRoomNumber("Z901").get();

        // same room number again -> 409
        mvc.perform(post("/api/rooms").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"roomNumber\":\"Z901\"}"))
                .andExpect(status().isConflict());

        mvc.perform(put("/api/rooms/" + room.getId()).with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"roomNumber\":\"Z901\",\"building\":\"Z\",\"floor\":9,\"monthlyRent\":4000,\"capacity\":1}"))
                .andExpect(status().isOk());
        assertEquals(4000.0, roomRepo.findById(room.getId()).get().getMonthlyRent());

        mvc.perform(delete("/api/rooms/" + room.getId()).with(admin())).andExpect(status().isNoContent());
        assertTrue(roomRepo.findByRoomNumber("Z901").isEmpty());
    }

    @Test
    public void testCreateTenantUnderRoom() throws Exception {
        Room room = roomRepo.findByRoomNumber("A103").get(); // empty room for 1 person

        mvc.perform(post("/api/rooms/" + room.getId() + "/tenants").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Test\",\"lastName\":\"User\",\"phone\":\"080-000-0000\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/rooms/" + room.getId() + "/tenants").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Test"));
        assertEquals(1, tenantRepo.countByRoom(room));

        // the room holds 1 person, so a second tenant -> 409
        mvc.perform(post("/api/rooms/" + room.getId() + "/tenants").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Second\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    public void testRoomInUseCannotBeDeleted() throws Exception {
        Room room = roomRepo.findByRoomNumber("A101").get(); // has tenants
        mvc.perform(delete("/api/rooms/" + room.getId()).with(admin())).andExpect(status().isConflict());
    }
}
