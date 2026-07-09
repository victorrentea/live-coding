// Ctrl/Cmd + mouse-wheel zoom for the IntelliJ Markdown preview.
// Injected into the JCEF preview document via MarkdownBrowserPreviewExtension.
//
// Why this works: the preview page loads ONCE and then only re-renders <body>
// content through IncrementalDOM as you type. We attach our listeners to `window`
// and set the zoom on <html> (document.documentElement), which is never recreated,
// so the zoom level survives edits and theme changes.
(function () {
  "use strict";

  var STORAGE_KEY = "victorrentea.md-preview-zoom";
  var MIN = 0.3;
  var MAX = 6.0;
  var STEP = 1.1; // ~10% per wheel notch

  function readZoom() {
    try {
      var v = parseFloat(window.localStorage.getItem(STORAGE_KEY));
      return isFinite(v) && v > 0 ? v : 1;
    } catch (e) {
      return 1;
    }
  }

  function saveZoom(z) {
    try {
      window.localStorage.setItem(STORAGE_KEY, String(z));
    } catch (e) {
      /* JCEF localStorage may be unavailable; keep zoom in-memory only. */
    }
  }

  function clamp(z) {
    return Math.min(MAX, Math.max(MIN, z));
  }

  var zoom = readZoom();

  function apply() {
    // `zoom` (Chromium) reflows content properly, unlike transform: scale().
    document.documentElement.style.zoom = zoom;
  }

  // Ctrl (Win/Linux) or Cmd (macOS) + wheel => zoom, and swallow Chromium's
  // own page-zoom so it doesn't fight us.
  window.addEventListener(
    "wheel",
    function (e) {
      if (!(e.ctrlKey || e.metaKey)) return;
      e.preventDefault();
      e.stopPropagation();
      zoom = clamp(zoom * (e.deltaY < 0 ? STEP : 1 / STEP));
      saveZoom(zoom);
      apply();
    },
    { passive: false, capture: true }
  );

  // Cmd/Ctrl + 0 resets to 100%.
  window.addEventListener(
    "keydown",
    function (e) {
      if ((e.ctrlKey || e.metaKey) && e.key === "0") {
        e.preventDefault();
        zoom = 1;
        saveZoom(zoom);
        apply();
      }
    },
    true
  );

  apply();
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", apply);
  }

  // Belt-and-suspenders: if a full re-render ever clears the <html> style,
  // re-assert briefly after load, then stop.
  var ticks = 0;
  var iv = setInterval(function () {
    apply();
    if (++ticks >= 12) clearInterval(iv);
  }, 250);
})();
