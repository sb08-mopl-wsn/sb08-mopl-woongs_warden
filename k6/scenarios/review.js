import http from 'k6/http';
import { check, sleep } from 'k6';
import { __VU, __ITER } from 'k6';
import { BASE_URL, getHeaders } from '../config/config.js';

const CONTENT_IDS = [
    "595d6c87-0135-403d-9a7d-346ce00807ba",
    "fe5ca754-bcbf-4822-99a4-e3b380f802e0",
    "e72a43d8-66e4-430c-b14e-1cd51104865a",
    "d697137d-2a07-4ce8-9776-d474d0aa1b3d"
];

export function reviewScenario(token, csrfToken) {
    const headers = getHeaders(token, csrfToken);

    const randomContentId = CONTENT_IDS[Math.floor(Math.random() * CONTENT_IDS.length)];

    // 1. 리뷰 목록 조회
    const listRes = http.get(`${BASE_URL}/api/reviews?contentId=${randomContentId}&limit=10`, {
        headers,
        tags: { name: 'GET /api/reviews' },
    });
    check(listRes, { '리뷰 목록 조회 성공': (r) => r.status === 200 });
    sleep(1);

    const randomRating = Math.floor(Math.random() * 5) + 1;
    const uniqueReviewText = `정말 재미있어요! (테스트 - VU:${__VU}, ITER:${__ITER}, Time:${Date.now()})`;

    // 2. 리뷰 생성
    const createPayload = JSON.stringify({
        contentId: randomContentId,
        text: uniqueReviewText,  // ✅ 완벽하게 수정됨
        rating: randomRating,
    });

    const createRes = http.post(`${BASE_URL}/api/reviews`, createPayload, {
        headers,
        tags: { name: 'POST /api/reviews' },
    });
    check(createRes, { '리뷰 생성 또는 중복 방지 성공': (r) => r.status === 201 || r.status === 409 });

    const newReviewId = createRes.json('id');
    sleep(1);

    // 생성에 성공했을 때만 상세 조회/수정/삭제 파이프라인 진행
    if (newReviewId) {
        // 3. 리뷰 단건 조회
        const detailRes = http.get(`${BASE_URL}/api/reviews/${newReviewId}`, {
            headers,
            tags: { name: 'GET /api/reviews/{reviewId}' },
        });
        check(detailRes, { '리뷰 단건 조회 성공': (r) => r.status === 200 });
        sleep(1);

        // 4. 리뷰 수정
        const updatePayload = JSON.stringify({
            text: `내용을 수정합니다. (수정됨 - VU:${__VU})`, // ✅ 여기도 text로 수정 완료!
            rating: randomRating === 5 ? 4 : 5,
        });

        const updateRes = http.patch(`${BASE_URL}/api/reviews/${newReviewId}`, updatePayload, {
            headers,
            tags: { name: 'PATCH /api/reviews/{reviewId}' },
        });
        check(updateRes, { '리뷰 수정 성공': (r) => r.status === 200 });
        sleep(1);

        // 5. 리뷰 삭제
        const deleteRes = http.del(`${BASE_URL}/api/reviews/${newReviewId}`, null, {
            headers,
            tags: { name: 'DELETE /api/reviews/{reviewId}' },
        });
        check(deleteRes, { '리뷰 삭제 성공': (r) => r.status === 204 });
        sleep(1);
    }
}