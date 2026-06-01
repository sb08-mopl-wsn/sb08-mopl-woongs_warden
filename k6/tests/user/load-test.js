import { sleep } from 'k6';
import { login } from '../../config/user_config.js';
import { userScenario } from '../../scenarios/user.js';

const stages = [
    { duration: '1m', target: 30 },
    { duration: '5m', target: 30 },
    { duration: '30s', target: 0 },
];

export const options = {
    scenarios: {
        user: {
            executor: 'ramping-vus',
            stages,
            exec: 'userTest',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1500'],
        http_req_failed: ['rate<0.01'],

        'http_req_duration{name:GET /api/users/{userId}}': ['p(95)<300'],
        'http_req_duration{name:PATCH /api/users/{userId}}': ['p(95)<500'],
    },
};

export function setup() {
    return login();
}

export function userTest(data) {
    userScenario(data);
    sleep(1);
}