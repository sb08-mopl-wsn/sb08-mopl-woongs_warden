package com.mopl.mopl.domain.follow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.follow.dto.FollowDto;
import com.mopl.mopl.domain.follow.dto.FollowRequest;
import com.mopl.mopl.domain.follow.service.FollowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowController 단위 테스트")
class FollowControllerTest {

    @InjectMocks
    private FollowController followController;

    @Mock
    private FollowService followService;

    @Mock
    private Environment env;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID followerId;
    private UUID followeeId;
    private UUID followId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(followController).build();
        objectMapper = new ObjectMapper();

        followerId = UUID.randomUUID();
        followeeId = UUID.randomUUID();
        followId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("validateTempAuthAllowed - prod 환경 인증 방어")
    class ValidateTempAuthAllowed {

        @Test
        @DisplayName("prod 프로파일이 활성화된 경우 follow 요청은 401을 반환한다.")
        void givenProdProfile_whenFollow_thenReturns401() throws Exception {
            given(env.getActiveProfiles()).willReturn(new String[]{"prod"});

            FollowRequest request = new FollowRequest(followeeId);

            mockMvc.perform(post("/api/follows")
                            .header("X-Temp-User-Id", followerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());

            verify(followService, never()).follow(any(), any());
        }

        @Test
        @DisplayName("prod 프로파일이 활성화된 경우 unfollow 요청은 401을 반환한다.")
        void givenProdProfile_whenUnfollow_thenReturns401() throws Exception {
            given(env.getActiveProfiles()).willReturn(new String[]{"prod"});

            mockMvc.perform(delete("/api/follows/{followId}", followId)
                            .header("X-Temp-User-Id", followerId.toString()))
                    .andExpect(status().isUnauthorized());

            verify(followService, never()).unfollow(any(), any());
        }

        @Test
        @DisplayName("prod 프로파일이 활성화된 경우 isFollowedByMe 요청은 401을 반환한다.")
        void givenProdProfile_whenIsFollowedByMe_thenReturns401() throws Exception {
            given(env.getActiveProfiles()).willReturn(new String[]{"prod"});

            mockMvc.perform(get("/api/follows/followed-by-me")
                            .header("X-Temp-User-Id", followerId.toString())
                            .param("followeeId", followeeId.toString()))
                    .andExpect(status().isUnauthorized());

            verify(followService, never()).isFollowedByMe(any(), any());
        }

        @Test
        @DisplayName("dev 프로파일이 활성화된 경우 follow 요청은 201을 반환한다.")
        void givenDevProfile_whenFollow_thenReturns201() throws Exception {
            given(env.getActiveProfiles()).willReturn(new String[]{"dev"});

            FollowRequest request = new FollowRequest(followeeId);
            FollowDto expectedDto = new FollowDto(followId, followeeId, followerId);
            given(followService.follow(eq(followerId), any(FollowRequest.class))).willReturn(expectedDto);

            mockMvc.perform(post("/api/follows")
                            .header("X-Temp-User-Id", followerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("여러 프로파일 중 prod가 포함된 경우 401을 반환한다.")
        void givenMultipleProfilesIncludingProd_whenFollow_thenReturns401() throws Exception {
            given(env.getActiveProfiles()).willReturn(new String[]{"dev", "prod"});

            FollowRequest request = new FollowRequest(followeeId);

            mockMvc.perform(post("/api/follows")
                            .header("X-Temp-User-Id", followerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("follow - 팔로우 요청")
    class Follow {

        @Test
        @DisplayName("유효한 요청으로 팔로우 시 201과 FollowDto를 반환한다.")
        void givenValidRequest_whenFollow_thenReturns201WithFollowDto() throws Exception {
            given(env.getActiveProfiles()).willReturn(new String[]{"dev"});

            FollowRequest request = new FollowRequest(followeeId);
            FollowDto expectedDto = new FollowDto(followId, followeeId, followerId);
            given(followService.follow(eq(followerId), any(FollowRequest.class))).willReturn(expectedDto);

            mockMvc.perform(post("/api/follows")
                            .header("X-Temp-User-Id", followerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.followerId").value(followerId.toString()))
                    .andExpect(jsonPath("$.followeeId").value(followeeId.toString()));
        }

        @Test
        @DisplayName("X-Temp-User-Id 헤더가 없으면 400을 반환한다.")
        void givenMissingHeader_whenFollow_thenReturns400() throws Exception {
            FollowRequest request = new FollowRequest(followeeId);

            mockMvc.perform(post("/api/follows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("unfollow - 언팔로우 요청")
    class Unfollow {

        @Test
        @DisplayName("유효한 요청으로 언팔로우 시 204를 반환한다.")
        void givenValidRequest_whenUnfollow_thenReturns204() throws Exception {
            given(env.getActiveProfiles()).willReturn(new String[]{"dev"});
            willDoNothing().given(followService).unfollow(followerId, followId);

            mockMvc.perform(delete("/api/follows/{followId}", followId)
                            .header("X-Temp-User-Id", followerId.toString()))
                    .andExpect(status().isNoContent());

            verify(followService).unfollow(followerId, followId);
        }

        @Test
        @DisplayName("X-Temp-User-Id 헤더가 없으면 400을 반환한다.")
        void givenMissingHeader_whenUnfollow_thenReturns400() throws Exception {
            mockMvc.perform(delete("/api/follows/{followId}", followId))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("isFollowedByMe - 팔로우 여부 확인")
    class IsFollowedByMe {

        @Test
        @DisplayName("팔로우 중이면 true를 반환한다.")
        void givenFollowing_whenIsFollowedByMe_thenReturnsTrue() throws Exception {
            given(env.getActiveProfiles()).willReturn(new String[]{"dev"});
            given(followService.isFollowedByMe(followerId, followeeId)).willReturn(true);

            mockMvc.perform(get("/api/follows/followed-by-me")
                            .header("X-Temp-User-Id", followerId.toString())
                            .param("followeeId", followeeId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(true));
        }

        @Test
        @DisplayName("팔로우 중이 아니면 false를 반환한다.")
        void givenNotFollowing_whenIsFollowedByMe_thenReturnsFalse() throws Exception {
            given(env.getActiveProfiles()).willReturn(new String[]{"dev"});
            given(followService.isFollowedByMe(followerId, followeeId)).willReturn(false);

            mockMvc.perform(get("/api/follows/followed-by-me")
                            .header("X-Temp-User-Id", followerId.toString())
                            .param("followeeId", followeeId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(false));
        }
    }

    @Nested
    @DisplayName("getFollowerCount - 팔로워 수 조회")
    class GetFollowerCount {

        @Test
        @DisplayName("팔로워 수를 정상적으로 반환한다. (프로파일 체크 없음)")
        void givenFolloweeId_whenGetFollowerCount_thenReturnsCount() throws Exception {
            long expectedCount = 42L;
            given(followService.getFollowerCount(followeeId)).willReturn(expectedCount);

            mockMvc.perform(get("/api/follows/count")
                            .param("followeeId", followeeId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(42));
        }

        @Test
        @DisplayName("getFollowerCount는 프로파일 체크 없이 동작한다 - prod 프로파일에서도 200을 반환한다.")
        void givenProdProfile_whenGetFollowerCount_thenReturns200() throws Exception {
            long expectedCount = 5L;
            given(followService.getFollowerCount(followeeId)).willReturn(expectedCount);

            // getFollowerCount does NOT call validateTempAuthAllowed, so env is never called
            mockMvc.perform(get("/api/follows/count")
                            .param("followeeId", followeeId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(5));

            verify(env, never()).getActiveProfiles();
        }
    }
}
