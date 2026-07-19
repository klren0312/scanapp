const test = require('node:test');
const assert = require('node:assert');
const { haversineMeters, wgs84ToGcj02, isInsideChina, toGcj02IfChina } = require('../src/geo');

test('haversineMeters: same point is ~0', () => {
  const a = { lat: 39.908, lng: 116.397 };
  assert.ok(haversineMeters(a, a) < 1);
});

test('haversineMeters: ~1km apart', () => {
  const a = { lat: 39.908, lng: 116.397 };
  const b = { lat: 39.917, lng: 116.397 };
  const d = haversineMeters(a, b);
  assert.ok(d > 900 && d < 1100, `expected ~1000m, got ${d}`);
});

test('isInsideChina: Beijing true, London false', () => {
  assert.equal(isInsideChina(39.908, 116.397), true);
  assert.equal(isInsideChina(51.5074, -0.1278), false);
});

test('wgs84ToGcj02: Beijing shifts east/north', () => {
  const r = wgs84ToGcj02(39.908, 116.397);
  assert.ok(r.lat > 39.908 && r.lng > 116.397);
  assert.ok(r.lng - 116.397 < 0.01);
});

test('toGcj02IfChina: outside China unchanged', () => {
  const r = toGcj02IfChina(51.5074, -0.1278);
  assert.equal(r.lat, 51.5074);
  assert.equal(r.lng, -0.1278);
});
