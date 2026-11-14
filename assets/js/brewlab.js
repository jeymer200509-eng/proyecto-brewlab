/* ==========================================================================
   BrewLab — Mejora progresiva de interfaz (Apple Fluid Interface Principles)
   - Respuesta inmediata en pointer-down con histéresis de cancelación (~10px)
   - Prevención de doble envío accidental en acciones críticas
   ========================================================================== */
(function () {
  'use strict';

  var HYSTERESIS_PX = 10;

  function initPointerFeedback() {
    var activeEl = null;
    var startX = 0;
    var startY = 0;

    document.addEventListener('pointerdown', function (e) {
      var target = e.target.closest ? e.target.closest('.btn, .nav-link') : null;
      if (!target || target.disabled) return;
      activeEl = target;
      startX = e.clientX;
      startY = e.clientY;
      target.classList.add('is-pressing');
    }, { passive: true });

    document.addEventListener('pointermove', function (e) {
      if (!activeEl) return;
      var dx = e.clientX - startX;
      var dy = e.clientY - startY;
      if (Math.hypot(dx, dy) > HYSTERESIS_PX) {
        var rect = activeEl.getBoundingClientRect();
        var inside = e.clientX >= rect.left - HYSTERESIS_PX &&
                     e.clientX <= rect.right + HYSTERESIS_PX &&
                     e.clientY >= rect.top - HYSTERESIS_PX &&
                     e.clientY <= rect.bottom + HYSTERESIS_PX;
        activeEl.classList.toggle('is-pressing', inside);
      }
    }, { passive: true });

    function clearPress() {
      if (activeEl) {
        activeEl.classList.remove('is-pressing');
        activeEl = null;
      }
    }

    document.addEventListener('pointerup', clearPress, { passive: true });
    document.addEventListener('pointercancel', clearPress, { passive: true });
  }

  function initAntiDoubleSubmit() {
    document.addEventListener('submit', function (e) {
      var form = e.target;
      if (!form || !form.hasAttribute('data-once')) return;
      if (form.getAttribute('data-submitted') === 'true') {
        e.preventDefault();
        return;
      }
      form.setAttribute('data-submitted', 'true');
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () {
      initPointerFeedback();
      initAntiDoubleSubmit();
    });
  } else {
    initPointerFeedback();
    initAntiDoubleSubmit();
  }
})();
