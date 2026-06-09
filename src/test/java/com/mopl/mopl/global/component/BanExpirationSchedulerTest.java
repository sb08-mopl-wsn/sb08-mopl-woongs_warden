package com.mopl.mopl.global.component;

import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BanExpirationScheduler 테스트")
public class BanExpirationSchedulerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserUnbanProcessor userUnbanProcessor;

    @InjectMocks
    private BanExpirationScheduler banExpirationScheduler;

    private User mockUser1;
    private User mockUser2;

    @BeforeEach
    void setUp() {
        mockUser1 = User.builder()
                .name("렉스")
                .email("rex@codeit.com")
                .password("test1234!")
                .build();
        ReflectionTestUtils.setField(mockUser1, "id", UUID.randomUUID());

        mockUser2 = User.builder()
                .name("우디")
                .email("woody@codeit.com")
                .password("test1234!")
                .build();
        ReflectionTestUtils.setField(mockUser2, "id", UUID.randomUUID());
    }

    @Test
    @DisplayName("정지 만료 기간이 지난 유저가 아예 없다면 스캔만 종료하고 정지 해제 프로세서를 호출하지 않는다.")
    void unbanExpiredUsers_emptyList_doesNothing() {
        // given
        given(userRepository.findAllByIsBannedTrueAndBanExpiresAtBeforeAndIsLockedFalse(any(LocalDateTime.class)))
                .willReturn(Collections.emptyList());

        // when
        banExpirationScheduler.unbanExpiredUsers();

        // then
        verify(userRepository, times(1)).findAllByIsBannedTrueAndBanExpiresAtBeforeAndIsLockedFalse(any());
        verifyNoInteractions(userUnbanProcessor);
    }

    @Test
    @DisplayName("정지 만료 기간이 지난 누수 유저들을 발견하면 순차적으로 모두 정지 해제(processUnban)를 실행한다.")
    void unbanExpiredUsers_success() {
        // given
        List<User> expiredUsers = List.of(mockUser1, mockUser2);
        given(userRepository.findAllByIsBannedTrueAndBanExpiresAtBeforeAndIsLockedFalse(any(LocalDateTime.class)))
                .willReturn(expiredUsers);

        // when
        banExpirationScheduler.unbanExpiredUsers();

        // then
        verify(userRepository, times(1)).findAllByIsBannedTrueAndBanExpiresAtBeforeAndIsLockedFalse(any());
        verify(userUnbanProcessor, times(1)).processUnban(mockUser1);
        verify(userUnbanProcessor, times(1)).processUnban(mockUser2);
    }

    @Test
    @DisplayName("유저들을 해제하는 루프 내부에서 특정 유저 처리에 예외가 터지더라도, 에러를 캐치하고 다음 유저의 해제를 마저 완수한다.")
    void unbanExpiredUsers_exceptionIsolated_continuesLoop() {
        // given
        List<User> expiredUsers = List.of(mockUser1, mockUser2);
        given(userRepository.findAllByIsBannedTrueAndBanExpiresAtBeforeAndIsLockedFalse(any(LocalDateTime.class)))
                .willReturn(expiredUsers);

        doThrow(new RuntimeException("인프라 에러")).when(userUnbanProcessor).processUnban(mockUser1);

        // when
        banExpirationScheduler.unbanExpiredUsers();

        // then
        verify(userRepository, times(1)).findAllByIsBannedTrueAndBanExpiresAtBeforeAndIsLockedFalse(any());
        verify(userUnbanProcessor, times(1)).processUnban(mockUser1);
        verify(userUnbanProcessor, times(1)).processUnban(mockUser2);
    }
}
