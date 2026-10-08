const SEEN_KEY = 'edulink.splashSeen';
/** How long the splash stays up on the first visit of a browser session, so its animation can play. */
const FIRST_VISIT_MS = 1800;

function seenThisSession() {
  try {
    return sessionStorage.getItem(SEEN_KEY) === '1';
  } catch {
    return false; // storage unavailable (e.g. private mode): treat every load as a first visit
  }
}

function markSeen() {
  try {
    sessionStorage.setItem(SEEN_KEY, '1');
  } catch {
    // not remembered, so the full splash plays again on the next reload
  }
}

/**
 * Fades out the splash screen from index.html once the app has rendered.
 * The first load of a session shows it for at least FIRST_VISIT_MS; later reloads hide it as soon as React is ready.
 */
export function hideSplash() {
  const splash = document.getElementById('splash');
  if (!splash) return;

  const started = window.__edulinkSplashStart ?? Date.now();
  const minimum = seenThisSession() ? 0 : FIRST_VISIT_MS;
  const wait = Math.max(0, minimum - (Date.now() - started));
  markSeen();

  setTimeout(() => {
    splash.classList.add('splash-hide');
    splash.setAttribute('aria-hidden', 'true');
    // Remove it after the fade; the timeout covers browsers that skip transitionend (e.g. reduced motion)
    const remove = () => splash.remove();
    splash.addEventListener('transitionend', remove, { once: true });
    setTimeout(remove, 800);
  }, wait);
}
