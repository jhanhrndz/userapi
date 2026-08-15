package com.jhan.userapi.performance;

import com.jhan.userapi.AbstractIntegrationTest;
import com.jhan.userapi.models.Role;
import com.jhan.userapi.models.User;
import com.jhan.userapi.repositorys.UserRepository;
import com.jhan.userapi.utils.TestDataBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ProtectedEndpointsLoadTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        User adminUser = new User();
        adminUser.setUsername("adminperf2");
        adminUser.setEmail("adminperf2@example.com");
        adminUser.setPassword(passwordEncoder.encode("AdminPass123!"));
        adminUser.setFirstName("Admin");
        adminUser.setLastName("Perf2");
        adminUser.setRole(Role.ADMIN);
        userRepository.save(adminUser);

        adminToken = TestDataBuilder.getAuthToken(mockMvc, objectMapper, "adminperf2", "AdminPass123!");
    }

    @Test
    void concurrentGetUsers_10Users() throws Exception {
        int threadCount = 10;
        int requestsPerThread = 3;
        int totalRequests = threadCount * requestsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        LongAdder totalLatency = new LongAdder();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        long start = System.nanoTime();
                        mockMvc.perform(get("/users")
                                        .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk());
                        totalLatency.add(System.nanoTime() - start);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Ignore errors
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "Test timed out");
        executor.shutdown();

        double avgLatencyMs = totalLatency.doubleValue() / successCount.get() / 1_000_000;
        double throughput = successCount.get() / 30.0;

        System.out.printf("GET /users Load Test: %d requests, %d successes, avg latency: %.2f ms, throughput: %.2f req/s%n",
                totalRequests, successCount.get(), avgLatencyMs, throughput);

        assertTrue(successCount.get() == totalRequests, "All requests should succeed");
        assertTrue(avgLatencyMs < 500, "Average latency should be under 500ms (test environment)");
    }

    @Test
    void concurrentGetUserById_10Users() throws Exception {
        int threadCount = 10;
        int requestsPerThread = 3;
        int totalRequests = threadCount * requestsPerThread;

        // Get the actual admin user ID
        Long adminUserId = userRepository.findByUsername("adminperf2").orElseThrow().getId();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        LongAdder totalLatency = new LongAdder();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        long start = System.nanoTime();
                        mockMvc.perform(get("/users/" + adminUserId)
                                        .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk());
                        totalLatency.add(System.nanoTime() - start);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Ignore errors
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "Test timed out");
        executor.shutdown();

        double avgLatencyMs = totalLatency.doubleValue() / successCount.get() / 1_000_000;
        double throughput = successCount.get() / 30.0;

        System.out.printf("GET /users/{id} Load Test: %d requests, %d successes, avg latency: %.2f ms, throughput: %.2f req/s%n",
                totalRequests, successCount.get(), avgLatencyMs, throughput);

        assertTrue(successCount.get() == totalRequests, "All requests should succeed");
        assertTrue(avgLatencyMs < 500, "Average latency should be under 500ms (test environment)");
    }
}