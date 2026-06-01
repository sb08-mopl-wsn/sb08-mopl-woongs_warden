import http from 'k6/http';
import ws from 'k6/ws'; // k6/websockets 가 아닌 k6/ws 가 올바른 모듈 이름입니다.
import { check, sleep } from 'k6';
import { BASE_URL, getHeaders } from '../config/config.js';

/**
 * 시청 세션 및 실시간 채팅 시나리오 (동적 ID 반영 및 STOMP 준수)
 */
export function watchingSessionScenario(token, csrfToken) {
    if (!token) {
        console.error("[에러] 인증 토큰이 없습니다.");
        return;
    }
    
    if (!BASE_URL) {
        console.error("[에러] BASE_URL이 정의되지 않았습니다.");
        return;
    }

    const headers = getHeaders(token, csrfToken);

    // 1. 콘텐츠 목록 조회하여 유효한 contentId 가져오기
    const listRes = http.get(`${BASE_URL}/api/contents?limit=1`, { 
        headers,
        tags: { name: 'GET /api/contents (for ID)' } 
    });
    
    let contentId = null;
    try {
        const body = JSON.parse(listRes.body);
        const contents = body.content || body.data || [];
        if (contents.length > 0) {
            contentId = contents[0].contentId || contents[0].id;
        }
    } catch (e) {
        console.error('[에러] 콘텐츠 목록 파싱 실패');
        return;
    }

    if (!contentId) {
        console.error('[에러] 시청할 콘텐츠가 없습니다. DB 데이터를 확인하세요.');
        return;
    }

    // 2. WebSocket URL 설정
    const wsUrl = (BASE_URL.startsWith('https')
        ? BASE_URL.replace('https', 'wss')
        : BASE_URL.replace('http', 'ws')) + '/ws/websocket';

    const NULL_CHAR = String.fromCharCode(0);
    console.log(`[웹소켓 시도] ContentID: ${contentId}, URL: ${wsUrl}`);

    // 3. WebSocket 연결 및 STOMP 통신
    try {
        const res = ws.connect(wsUrl, { headers: { 'Authorization': `Bearer ${token}` } }, function (socket) {
            if (!socket) {
                console.error("[웹소켓 에러] 소켓 객체가 생성되지 않았습니다.");
                return;
            }

            socket.on('open', function () {
                console.log('[STOMP] WebSocket Open. Sending CONNECT...');
                const connectFrame = [
                    'CONNECT',
                    'accept-version:1.2,1.1,1.0',
                    'heart-beat:10000,10000',
                    `Authorization:Bearer ${token}`,
                    '',
                    NULL_CHAR
                ].join('\n');
                socket.send(connectFrame);
            });

            socket.on('message', function (message) {
                const msgString = typeof message === 'string' ? message : message.data;

                if (msgString.startsWith('CONNECTED')) {
                    console.log(`[STOMP] Connected Successfully`);

                    // SUBSCRIBE 시청
                    socket.send(['SUBSCRIBE', 'id:sub-' + Math.random().toString(36).substring(7), `destination:/sub/contents/${contentId}/watch`, '', NULL_CHAR].join('\n'));
                    
                    // SUBSCRIBE 채팅
                    socket.send(['SUBSCRIBE', 'id:sub-' + Math.random().toString(36).substring(7), `destination:/sub/contents/${contentId}/chat`, '', NULL_CHAR].join('\n'));

                    sleep(1);

                    // SEND 채팅
                    const chatPayload = JSON.stringify({ content: '실시간 채팅 부하 테스트 메시지' });
                    const sendFrame = [
                        'SEND',
                        `destination:/pub/contents/${contentId}/chat`,
                        'content-type:application/json',
                        '',
                        chatPayload,
                        NULL_CHAR
                    ].join('\n');
                    
                    socket.send(sendFrame);
                    console.log(`[STOMP] Message Sent to /pub/contents/${contentId}/chat`);

                    sleep(5);
                    
                    const disconnectFrame = ['DISCONNECT', '', NULL_CHAR].join('\n');
                    socket.send(disconnectFrame);
                    socket.close();
                }
            });

            socket.on('error', function (e) {
                console.log('[웹소켓 에러]', e && e.error ? e.error() : JSON.stringify(e));
            });

            socket.on('close', function () {
                console.log('[웹소켓 연결 종료]');
            });
        });

        if (res && res.status !== 101) {
            console.error(`[웹소켓 핸드셰이크 실패] Status: ${res.status}, Body: ${res.body}`);
        }

        check(res, {
            'WebSocket Handshake 성공 (101)': (r) => r && r.status === 101,
        });
    } catch (err) {
        console.error(`[런타임 에러] 웹소켓 실행 중 에러: ${err}`);
    }
}
