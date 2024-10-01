package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.UnexpectedRollbackException;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class MemberServiceTest {

    @Autowired
    MemberService memberService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    LogRepository logRepository;

    /**
     *  memberService       @Transactional : OFF
     *  memberRepository    @Transactional : ON
     *  logRepository       @Transactional : ON
     */
    @Test
    void outerTxOff_success() {
        // given
        String username = "outerTxOff_success";

        // when
        memberService.joinV1(username);

        // then : 모든 데이터가 정상 저장된다.
        Assertions.assertTrue(memberRepository.find(username).isPresent());
        Assertions.assertTrue(logRepository.find(username).isPresent());

    }

    /**
     *  memberService       @Transactional : OFF
     *  memberRepository    @Transactional : ON
     *  logRepository       @Transactional : ON Exception
     */
    @Test
    void outerTxOff_fail() {
        // given
        String username = "로그예외 outerTxOff_fail";

        // when
        Assertions.assertThrows(RuntimeException.class, () -> memberService.joinV1(username));

        // then : member만 저장된다.
        Assertions.assertTrue(memberRepository.find(username).isPresent());
        Assertions.assertTrue(logRepository.find(username).isEmpty());
    }

    /**
     *  memberService       @Transactional : ON
     *  memberRepository    @Transactional : OFF
     *  logRepository       @Transactional : OFF
     *  한개의 트랜잭션에서 실행
     */
    @Test
    void singleTx() {
        // given
        String username = "singleTx";

        // when
        memberService.joinV1(username);

        // then : 모든 데이터가 정상 저장된다.
        Assertions.assertTrue(memberRepository.find(username).isPresent());
        Assertions.assertTrue(logRepository.find(username).isPresent());
    }

    /**
     *  memberService       @Transactional : ON
     *  memberRepository    @Transactional : ON
     *  logRepository       @Transactional : ON
     *  한개의 트랜잭션에서 실행
     */
    @Test
    void outerTxOn_success() {
        // given
        String username = "outerTxOn_success";

        // when
        memberService.joinV1(username);

        // then : 모든 데이터가 정상 저장된다.
        Assertions.assertTrue(memberRepository.find(username).isPresent());
        Assertions.assertTrue(logRepository.find(username).isPresent());
    }

    /**
     *  memberService       @Transactional : ON
     *  memberRepository    @Transactional : ON
     *  logRepository       @Transactional : ON Exception
     */
    @Test
    void recoverException_fail() {
        // given
        String username = "로그예외 recoverException_fail";

        // when
        Assertions.assertThrows(UnexpectedRollbackException.class, ()->memberService.joinV2(username));

        // then : 모든 데이터가 롤백 된다.
        Assertions.assertTrue(memberRepository.find(username).isEmpty());
        Assertions.assertTrue(logRepository.find(username).isEmpty());
    }

    /**
     *  memberService       @Transactional : ON
     *  memberRepository    @Transactional : ON
     *  logRepository       @Transactional : ON(REQUIRES_NEW) Exception
     */
    @Test
    void recoverException_success() {
        // given
        String username = "로그예외 recoverException_success";

        // when
        memberService.joinV2(username);

        // then : 모든 데이터가 롤백 된다.
        Assertions.assertTrue(memberRepository.find(username).isPresent());
        Assertions.assertTrue(logRepository.find(username).isEmpty());
    }
}