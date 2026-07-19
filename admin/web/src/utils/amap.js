const KEY = import.meta.env.VITE_AMAP_KEY;
const SECURITY = import.meta.env.VITE_AMAP_SECURITY_CODE;
let promise = null;

export function loadAMap() {
  if (window.AMap) return Promise.resolve(window.AMap);
  if (promise) return promise;
  if (SECURITY) {
    window._AMapSecurityConfig = { securityJsCode: SECURITY };
  }
  promise = new Promise((resolve, reject) => {
    const s = document.createElement('script');
    s.src = `https://webapi.amap.com/maps?v=2.0&key=${KEY}&plugin=AMap.Polyline`;
    s.async = true;
    s.onload = () => (window.AMap ? resolve(window.AMap) : reject(new Error('AMap load failed')));
    s.onerror = () => reject(new Error('AMap script error'));
    document.head.appendChild(s);
  });
  return promise;
}
