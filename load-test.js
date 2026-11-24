// k6 Load Testing Script for Athenaeum LMS
// Validates NFR-01: Response time < 2.0s for 95% of requests under concurrent load

import http from 'k6/http';
import { check, sleep, group } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 }, // Ramp-up to 20 VUs
    { duration: '1m', target: 50 },  // Sustained load at 50 VUs
    { duration: '30s', target: 0 },  // Ramp-down
  ],
  thresholds: {
    // NFR-01: 95% of requests must complete in under 2000ms
    http_req_duration: ['p(95)<2000', 'p(99)<3500'],
    // System reliability: Error rate must be under 1%
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  // Scenario 1: Patron Discovery Search
  group('Catalog Search & Faceting', function () {
    const searchQueries = ['algorithms', 'clean', 'java', 'physics', 'design'];
    const query = searchQueries[Math.floor(Math.random() * searchQueries.length)];
    
    const searchRes = http.get(`${BASE_URL}/catalog?q=${query}`);
    check(searchRes, {
      'catalog search status is 200': (r) => r.status === 200,
      'catalog search responds under 2s': (r) => r.timings.duration < 2000,
    });
    sleep(1);

    // Filter by Subject Facet
    const facetRes = http.get(`${BASE_URL}/catalog?category=Computer+Science&sortBy=newest`);
    check(facetRes, {
      'facet query status is 200': (r) => r.status === 200,
      'facet query responds under 2s': (r) => r.timings.duration < 2000,
    });
    sleep(1);
  });

  // Scenario 2: API Catalog Query (Headless & REST consumer)
  group('REST API Discovery', function () {
    const apiRes = http.get(`${BASE_URL}/api/v1/catalog/search?q=code&availableOnly=true`);
    check(apiRes, {
      'api search status is 200': (r) => r.status === 200,
      'api search duration < 500ms': (r) => r.timings.duration < 500,
    });
    sleep(1);
  });

  // Scenario 3: Health & Readiness Probe
  group('System Health Readiness', function () {
    const healthRes = http.get(`${BASE_URL}/actuator/health`);
    check(healthRes, {
      'actuator health status is 200': (r) => r.status === 200,
    });
    sleep(1);
  });
}
