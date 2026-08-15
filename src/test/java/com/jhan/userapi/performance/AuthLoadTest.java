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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthLoadTest extends AbstractIntegrationTest {

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
        adminUser.setUsername("adminperf");
        adminUser.setEmail("adminperf@example.com");
        adminUser.setPassword(passwordEncoder.encode("AdminPass123!"));
        adminUser.setFirstName("Admin");
        adminUser.setLastName("Perf");
        adminUser.setRole(Role.ADMIN);
        userRepository.save(adminUser);

        adminToken = TestDataBuilder.getAuthToken(mockMvc, objectMapper, "adminperf", "AdminPass123!");
    }

    @Test
    void concurrentLogin_5Users() throws Exception {
        int threadCount = 5;
        int requestsPerThread = 3;
        int totalRequests = threadCount * requestsPerThread;

        for (int i = 0; i < threadCount; i++) {
            User user = new User();
            user.setUsername("perfuser" + i);
            user.setEmail("perfuser" + i + "@example.com");
            user.setPassword(passwordEncoder.encode("Password123!"));
            user.setFirstName("Perf");
            user.setLastName("User" + i);
            user.setRole(Role.USER);
            userRepository.save(user);
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        LongAdder totalLatency = new LongAdder();

        for (int i = 0; i < threadCount; i++) {
            final int userIndex = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        long start = System.nanoTime();
                        mockMvc.perform(post("/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"username\":\"perfuser" + userIndex + "\",\"password\":\"Password123!\"}"))
                                .andExpect(status().isOk());
                        totalLatency.add(System.nanoTime() - start);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Ignore errors for load test
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "Test timed out");
        executor.shutdown();

        double avgLatencyMs = totalLatency.doubleValue() / successCount.get() / 1_000_000;
        System.out.printf("Login Load Test: %d requests, %d successes, avg latency: %.2f ms%n",
                totalRequests, successCount.get(), avgLatencyMs);

        assertTrue(successCount.get() == totalRequests, "All requests should succeed");
        assertTrue(avgLatencyMs < 2000, "Average latency should be under 2000ms (test environment)");
    }

    @Test
    void concurrentRegister_5Users() throws Exception {
        int threadCount = 5;
        int requestsPerThread = 2;
        int totalRequests = threadCount * requestsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        LongAdder totalLatency = new LongAdder();

        for (int i = 0; i < threadCount; i++) {
            final int userIndex = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        long start = System.nanoTime();
                        String unique = userIndex + "_" + j + "_" + System.nanoTime();
                        mockMvc.perform(post("/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"username\":\"reguser" + unique + "\",\"email\":\"reg" + unique + "@example.com\",\"password\":\"Password123!\",\"firstName\":\"Reg\",\"lastName\":\"User\"}"))
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
        System.out.printf("Register Load Test: %d requests, %d successes, avg latency: %.2f ms%n",
                totalRequests, successCount.get(), avgLatencyMs);

        assertTrue(successCount.get() == totalRequests, "All requests should succeed");
        assertTrue(avgLatencyMs < 3000, "Average latency should be under 3000ms (test environment)");
    }

    @Test
    void sustainedLoad_5seconds() throws Exception {
        int threadCount = 3;
        int durationSeconds = 5;

        for (int i = 0; i < 5; i++) {
            User user = new User();
            user.setUsername("sustaineduser" + i);
            user.setEmail("sustained" + i + "@example.com");
            user.setPassword(passwordEncoder.encode("Password123!"));
            user.setFirstName("Sustained");
            user.setLastName("User" + i);
            user.setRole(Role.USER);
            userRepository.save(user);
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        LongAdder totalRequests = new LongAdder();
        LongAdder successRequests = new LongAdder();
        LongAdder totalLatency = new LongAdder();

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationSeconds * 1000L);

        for (int i = 0; i < threadCount; i++) {
            final int userIndex = i % 5;
            executor.submit(() -> {
                try {
                    while (System.currentTimeMillis() < endTime) {
                        long start = System.nanoTime();
                        try {
                            mockMvc.perform(post("/auth/login")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content("{\"username\":\"sustaineduser" + userIndex + "\",\"password\":\"Password123!\"}"))
                                    .andExpect(status().isOk());
                            successRequests.increment();
                        } catch (Exception e) {
                            // Ignore errors
                        }
                        totalLatency.add(System.nanoTime() - start);
                        totalRequests.increment();
                        Thread.sleep(50);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(durationSeconds + 10, TimeUnit.SECONDS), "Test timed out");
        executor.shutdown();

        double avgLatencyMs = totalLatency.doubleValue() / successRequests.doubleValue() / 1_000_000;
        double throughput = successRequests.doubleValue() / durationSeconds;

        System.out.printf("Sustained Load Test: %d total, %d successes, avg latency: %.2f ms, throughput: %.2f req/s%n",
                totalRequests.longValue(), successRequests.longValue(), avgLatencyMs, throughput);

        assertTrue(successRequests.longValue() > 0, "Should have successful requests");
        assertTrue(avgLatencyMs < 2000, "Average latency should be under 2000ms (test environment)");
    }
}