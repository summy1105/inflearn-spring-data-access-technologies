package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import javax.sql.DataSource;

@Slf4j
@SpringBootTest
public class BasicTxTest {

    @Autowired
    PlatformTransactionManager txManager;

    @TestConfiguration
    static class Config {
        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    @Test
    void commit() {
        log.info("트랜잭션 시작");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션 커밋 시작");
        txManager.commit(status);
        log.info("트랜잭션 커밋 완료");
    }

    @Test
    void rollback() {
        log.info("트랜잭션 시작");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션 롤백 시작");
        txManager.rollback(status);
        log.info("트랜잭션 롤백 완료");
    }

    @Test
    void double_commit() {
        log.info("트랜잭션1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션1 커밋");
        txManager.commit(tx1);

        log.info("트랜잭션2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션2 커밋");
        txManager.commit(tx2);
    }

    @Test
    void double_commit_rollback() {
        log.info("트랜잭션1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션1 커밋");
        txManager.commit(tx1);

        log.info("트랜잭션2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션2 커밋");
        txManager.rollback(tx2);
    }

    @Test
    void inner_commit() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outerTx = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outerTx.isNewTransaction()={}", outerTx.isNewTransaction());

        { // 내부 트랜잭션은 외부 트랜잭션에 참여 -> 외부의 물리 트랜잭션을 그래도 이어받아 사용
            log.info("내부 트랜잭션 시작");
            TransactionStatus innerTx = txManager.getTransaction(new DefaultTransactionAttribute());
            log.info("innerTx.isNewTransaction()={}", innerTx.isNewTransaction());
            // log: innerTx.isNewTransaction()=false

            log.info("내부 트랜잭션 커밋");
            txManager.commit(innerTx); // 외부 트랜잭션 커밋전에 관련로그 없음 -> 외부의 커밋전까지 작업하지 않는다.
        }

        log.info("외부 트랜잭션 커밋");
        txManager.commit(outerTx);

    }

    @Test
    void outer_rollbaock() { // 외부 롤백 -> 외, 내부 상관없이 전체 롤백
        log.info("외부 트랜잭션 시작");
        TransactionStatus outerTx = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outerTx.isNewTransaction()={}", outerTx.isNewTransaction());

        {
            log.info("내부 트랜잭션 시작");
            TransactionStatus innerTx = txManager.getTransaction(new DefaultTransactionAttribute());
            log.info("내부 트랜잭션 커밋");
            txManager.commit(innerTx); // 외부 트랜잭션 커밋전에 관련로그 없음 -> 외부의 커밋전까지 작업하지 않는다.
        }

        log.info("외부 트랜잭션 롤백");
        txManager.rollback(outerTx);
    }

    @Test
    void inner_rollback() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outerTx = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outerTx.isNewTransaction()={}", outerTx.isNewTransaction());

        {
            log.info("내부 트랜잭션 시작");
            TransactionStatus innerTx = txManager.getTransaction(new DefaultTransactionAttribute());
            log.info("내부 트랜잭션 롤백");
            txManager.rollback(innerTx); // 내부에서 롤백을 하면 현재 참여하는 트랜잭션에 rollback 마크함
            // Participating transaction failed - marking existing transaction as rollback-only
        }

        log.info("외부 트랜잭션 커밋");
        Assertions.assertThatThrownBy(() ->txManager.commit(outerTx)) // 논리 트랜잭션이 하나라도 롤백되면 물리트랜잭션은 롤백되고
                // 혹시라도 commit이 된다면, 롤백됨을 확인해야 하기 때문에 예외를 발생함
                .isInstanceOf(UnexpectedRollbackException.class);
    }

    @Test
    void inner_rollback_requires_new() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outerTx = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outerTx.isNewTransaction()={}", outerTx.isNewTransaction());

        {
            log.info("내부 트랜잭션 시작");
            DefaultTransactionAttribute definition = new DefaultTransactionAttribute();
            definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            TransactionStatus innerTx = txManager.getTransaction(definition);
            log.info("innerTx.isNewTransaction()={}", innerTx.isNewTransaction()); // true

            log.info("내부 트랜잭션 롤백");
            txManager.rollback(innerTx);
        }

        log.info("외부 트랜잭션 커밋");
        txManager.commit(outerTx);
    }
}
