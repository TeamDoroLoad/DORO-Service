package com.doroload.api.common.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Nearby 검색처럼 서로 독립적인 블로킹 DB/Redis 호출을 병렬로 실행해 순차 호출 시 누적되는
// 왕복 지연을 줄이는 데 쓰는 Virtual Thread Executor. 빈 소멸 시 close()가 자동 호출된다
// (ExecutorService가 AutoCloseable을 구현하므로 Spring이 별도 destroyMethod 지정 없이 감지).
@Configuration
public class AsyncExecutorConfig {

    @Bean
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
