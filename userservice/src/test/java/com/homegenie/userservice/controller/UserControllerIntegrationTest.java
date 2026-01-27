import com.fasterxml.jackson.databind.ObjectMapper;
import com.homegenie.userservice.dto.LoginRequest;
import com.homegenie.userservice.dto.RegisterRequest;
import com.homegenie.userservice.model.User;
import com.homegenie.userservice.model.UserRole;
import com.homegenie.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminJwtToken;
    private String userJwtToken;
    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        // Register and login an admin user
        adminUser = new User();
        adminUser.setFullName("Admin User");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode("adminpass"));
        adminUser.setPhoneNumber("1234567890");
        adminUser.setRole(UserRole.ADMIN);
        userRepository.save(adminUser);

        LoginRequest adminLogin = new LoginRequest();
        adminLogin.setEmail("admin@example.com");
        adminLogin.setPassword("adminpass");
        MvcResult adminLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andReturn();
        adminJwtToken = objectMapper.readTree(adminLoginResult.getResponse().getContentAsString()).get("jwt").asText();

        // Register and login a regular user
        regularUser = new User();
        regularUser.setFullName("Regular User");
        regularUser.setEmail("user@example.com");
        regularUser.setPassword(passwordEncoder.encode("userpass"));
        regularUser.setPhoneNumber("0987654321");
        regularUser.setRole(UserRole.RESIDENT);
        userRepository.save(regularUser);

        LoginRequest userLogin = new LoginRequest();
        userLogin.setEmail("user@example.com");
        userLogin.setPassword("userpass");
        MvcResult userLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userLogin)))
                .andReturn();
        userJwtToken = objectMapper.readTree(userLoginResult.getResponse().getContentAsString()).get("jwt").asText();
    }

    @Test
    void getCurrentUser_Authenticated() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + userJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.firstName").value("Regular"));
    }

    @Test
    void getCurrentUser_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden()); // Expect 403 Forbidden without token
    }

    @Test
    void getUserById_AdminAccess_Success() throws Exception {
        mockMvc.perform(get("/api/users/{id}", regularUser.getId())
                        .header("Authorization", "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void getUserById_UserAccess_Forbidden() throws Exception {
        // Regular user trying to access another user's info
        mockMvc.perform(get("/api/users/{id}", adminUser.getId())
                        .header("Authorization", "Bearer " + userJwtToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_AdminAccess_Success() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)); // Expect both admin and regular user
    }

    @Test
    void getAllUsers_UserAccess_Forbidden() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + userJwtToken))
                .andExpect(status().isForbidden());
    }
}
