package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@SpringBootTest
public class InternalCallV2Test {

    @Autowired
    CallService callService;

    @Test
    void printProxy() {
        log.info("callService class={}", callService.getClass());
    }

    @Test
    void externalCalV2l() {
        callService.external();
    }

    @TestConfiguration
    static class InternalCallV2TestConfig {

        @Bean CallService callService() {
            return new CallService(internalService());
        }

        @Bean InternalService internalService() {
            return new InternalService();
        }
    }


    static class CallService {

        private final InternalService internal;

        public CallService(InternalService internal) {
            this.internal = internal;
        }

        public void external() {
            log.info("call external");
            printTxInfo();
            internal.internal();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isSynchronizationActive();
            log.info("tx active={}", txActive);
            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            log.info("tx readOnly={}", readOnly);
        }
    }

    static class InternalService {

        @Transactional // 내부호출은 proxy로 감싼 이스턴스가 아니기 때문에 transaction template로 동작하지 않음
        public void internal() { // public에서만 transaction이 생성됨.
            log.info("call internal");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isSynchronizationActive();
            log.info("tx active={}", txActive);
            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            log.info("tx readOnly={}", readOnly);
        }
    }
}
