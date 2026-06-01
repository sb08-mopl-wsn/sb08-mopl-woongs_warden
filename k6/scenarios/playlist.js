import http from 'k6/http';
import { check, sleep } from 'k6';
import { __VU, __ITER } from 'k6';
import { BASE_URL, getHeaders } from '../config/config.js';

// 리뷰와 동일한 콘텐츠 ID 목록을 사용합니다.
const CONTENT_IDS = [
    "595d6c87-0135-403d-9a7d-346ce00807ba",
    "fe5ca754-bcbf-4822-99a4-e3b380f802e0",
    "e72a43d8-66e4-430c-b14e-1cd51104865a",
    "d697137d-2a07-4ce8-9776-d474d0aa1b3d"
];

const TARGET_PLAYLIST_ID = "2c924d60-bb27-46b7-bb6c-70fc863f3e91";

export function playlistScenario(token, csrfToken) {
    const headers = getHeaders(token, csrfToken);

    // 1. 플리 목록 조회
    const listRes = http.get(`${BASE_URL}/api/playlists?limit=10`, {
        headers,
        tags: { name: 'GET /api/playlists' },
    });
    check(listRes, { '플리 목록 조회 성공': (r) => r.status === 200 });
    sleep(1);

    // 2. 플리 생성 (고유한 제목)
    const uniqueTitle = `부하테스트 - VU:${__VU}, Time:${Date.now()}`;
    const createPayload = JSON.stringify({
        title: uniqueTitle,
        description: "부하 테스트를 위해 생성된 플레이리스트 설명입니다.",
        isPublic: true,
    });

    const createRes = http.post(`${BASE_URL}/api/playlists`, createPayload, {
        headers,
        tags: { name: 'POST /api/playlists' },
    });

    check(createRes, { '플리 생성 성공': (r) => r.status === 201 });

    if (createRes.status !== 201) {
        console.log(`[🚨생성 실패🚨] 상태코드: ${createRes.status}, 에러메시지: ${createRes.body}`);
    }

    const newPlaylistId = createRes.json('id');
    sleep(1);

    if (newPlaylistId) {
        // 3. 플리 단건 조회
        const detailRes = http.get(`${BASE_URL}/api/playlists/${newPlaylistId}`, {
            headers,
            tags: { name: 'GET /api/playlists/{playlistId}' },
        });
        check(detailRes, { '플리 단건 조회 성공': (r) => r.status === 200 });
        sleep(1);

        // 4. 플리에 콘텐츠 추가 (랜덤 ID 사용)
        const randomContentId = CONTENT_IDS[Math.floor(Math.random() * CONTENT_IDS.length)];
        const addContentRes = http.post(`${BASE_URL}/api/playlists/${newPlaylistId}/contents/${randomContentId}`, null, {
            headers,
            tags: { name: 'POST /api/playlists/{playlistId}/contents/{contentId}' },
        });
        check(addContentRes, { '플리 콘텐츠 추가 성공': (r) => r.status === 204 });
        sleep(1);

        // 5. 플리 구독
        const subscribeRes = http.post(`${BASE_URL}/api/playlists/${TARGET_PLAYLIST_ID}/subscription`, null, {
            headers,
            tags: { name: 'POST /api/playlists/{playlistId}/subscription' },
        });
        check(subscribeRes, {
            '플리 구독 성공 (또는 중복 방어)': (r) => r.status === 204 || r.status === 400 || r.status === 409
        });
        sleep(1);

        // 6. 플리 구독 취소
        const unsubscribeRes = http.del(`${BASE_URL}/api/playlists/${TARGET_PLAYLIST_ID}/subscription`, null, {
            headers,
            tags: { name: 'DELETE /api/playlists/{playlistId}/subscription' },
        });
        check(unsubscribeRes, {
            '플리 구독 취소 성공 (또는 미구독 방어)': (r) => r.status === 204 || r.status === 400 || r.status === 404
        });
        sleep(1);

        // 7. 플리에서 콘텐츠 삭제
        const removeContentRes = http.del(`${BASE_URL}/api/playlists/${newPlaylistId}/contents/${randomContentId}`, null, {
            headers,
            tags: { name: 'DELETE /api/playlists/{playlistId}/contents/{contentId}' },
        });
        check(removeContentRes, { '플리 콘텐츠 삭제 성공': (r) => r.status === 204 });
        sleep(1);

        // 8. 플리 수정
        const updatedTitle = `수정된 플리 - VU:${__VU}, Time:${Date.now()}`;
        const updatePayload = JSON.stringify({
            title: updatedTitle,
            description: "부하 테스트를 위해 생성된 플레이리스트 설명입니다.",
            isPublic: false,
        });
        const updateRes = http.patch(`${BASE_URL}/api/playlists/${newPlaylistId}`, updatePayload, {
            headers,
            tags: { name: 'PATCH /api/playlists/{playlistId}' },
        });
        check(updateRes, { '플리 수정 성공': (r) => r.status === 200 });
        sleep(1);

        // 9. 플리 삭제
        const deleteRes = http.del(`${BASE_URL}/api/playlists/${newPlaylistId}`, null, {
            headers,
            tags: { name: 'DELETE /api/playlists/{playlistId}' },
        });
        check(deleteRes, { '플리 삭제 성공': (r) => r.status === 204 });
        sleep(1);
    }
}