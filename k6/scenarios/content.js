import http from 'k6/http';
import {check, sleep} from 'k6';
import {BASE_URL, getHeaders} from '../config/config.js';

/**
 * 콘텐츠 도메인 시나리오
 *
 * 흐름:
 * 1. 콘텐츠 목록 조회 (커서 페이지네이션)
 * 2. 콘텐츠 단건 조회
 * 3. 콘텐츠 키워드 검색 (OpenSearch)
 */
export function contentScenario(token, csrfToken) {
    const headers = getHeaders(token, csrfToken);

    // 1. 콘텐츠 목록 조회
    const listRes = http.get(`${BASE_URL}/api/contents?limit=20`, {
        headers,
        tags: { name: 'GET /api/contents' },
    });

    check(listRes, {
        '콘텐츠 목록 조회 성공': (r) => r.status === 200,
    });

    if (listRes.status !== 200) {
        console.log('[목록 조회 실패] status=' + listRes.status);
    }

    sleep(1);

    // 2. 콘텐츠 단건 조회 — 목록에서 첫 번째 항목 ID 사용
    let contentId = null;
    try {
        const body = JSON.parse(listRes.body);
        const contents = body.content || body.data || [];
        if (contents.length > 0) {
            contentId = contents[0].contentId || contents[0].id;
        }
    } catch {
        // 파싱 실패 시 스킵
    }

    if (contentId) {
        const detailRes = http.get(`${BASE_URL}/api/contents/${contentId}`, {
            headers,
            tags: { name: 'GET /api/contents/{contentId}' },
        });

        check(detailRes, {
            '콘텐츠 단건 조회 성공': (r) => r.status === 200,
        });

        if (detailRes.status !== 200) {
            console.log('[단건 조회 실패] status=' + detailRes.status);
        }
    }

    sleep(1);

    // 3. 콘텐츠 키워드 검색 (OpenSearch)
    const keywords = ['action', 'drama', 'premier league', '영화', '스포츠'];
    const keyword = keywords[Math.floor(Math.random() * keywords.length)];

    const searchRes = http.get(
        `${BASE_URL}/api/contents?keyword=${encodeURIComponent(keyword)}&limit=20`,
        {
            headers,
            tags: { name: 'GET /api/contents?keyword=' },
        }
    );

    check(searchRes, {
        '콘텐츠 검색 성공': (r) => r.status === 200,
    });

    if (searchRes.status !== 200) {
        console.log('[검색 실패] status=' + searchRes.status);
    }

    sleep(1);
}