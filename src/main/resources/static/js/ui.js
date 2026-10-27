/*
 * DormHub shared UI behaviour (design system). Loaded once by every layout.
 *
 *  data-confirm="ลบห้อง A101?"        on <a> or a submit <button>: asks in the confirm modal first
 *      data-confirm-title, data-confirm-ok (button label, default "ลบ"),
 *      data-confirm-tone="primary" for a non-destructive confirmation (default "danger")
 *  POST forms                          the submit button shows a spinner and is disabled while sending
 *      add data-no-loading to a form to opt out
 *  <table class="table-cards">         each cell gets data-label from its column header, so the row
 *                                      can be shown as a stacked card on phones (see app.css)
 *  data-filter-target="#listId"        on a search <input>: hides [data-row] elements inside #listId
 *                                      (table rows or list items) that do not contain the text
 *  data-filter-key="status"            on a <select> with data-filter-target: keeps rows whose
 *                                      data-status contains the value as a token ("" = all)
 *  data-filter-count="#tableId"        element that shows "N รายการ"
 *  data-filter-empty="#tableId"        element shown when no row matches
 */
(function () {
    'use strict';

    /* ---------- icons: decorative, hidden from screen readers ---------- */
    if (window.lucide) {
        window.lucide.createIcons({ attrs: { 'aria-hidden': 'true' } });
    }

    /* ---------- confirm modal ---------- */
    var modalEl = document.getElementById('confirmModal');
    var modal = modalEl && window.bootstrap ? new window.bootstrap.Modal(modalEl) : null;
    var pending = null;

    document.addEventListener('click', function (e) {
        var trigger = e.target.closest('[data-confirm]');
        if (!trigger || trigger.dataset.confirmed === 'yes') {
            return;
        }
        e.preventDefault();
        var message = trigger.getAttribute('data-confirm');
        if (!modal) {
            // no modal on this page: fall back to the browser dialog
            if (window.confirm(message)) {
                proceed(trigger);
            }
            return;
        }
        modalEl.querySelector('[data-confirm-title]').textContent = trigger.getAttribute('data-confirm-title') || 'ยืนยันการลบ';
        modalEl.querySelector('[data-confirm-message]').textContent = message;
        var okButton = modalEl.querySelector('[data-confirm-ok]');
        okButton.textContent = trigger.getAttribute('data-confirm-ok') || 'ลบ';
        // tone "danger" (default) = delete style with the trash icon; "primary" = normal confirmation
        var danger = (trigger.getAttribute('data-confirm-tone') || 'danger') === 'danger';
        okButton.classList.toggle('btn-danger', danger);
        okButton.classList.toggle('btn-primary', !danger);
        modalEl.querySelector('.modal-icon').hidden = !danger;
        pending = trigger;
        modal.show();
    });

    if (modalEl) {
        modalEl.querySelector('[data-confirm-ok]').addEventListener('click', function () {
            var trigger = pending;
            pending = null;
            modal.hide();
            if (trigger) {
                proceed(trigger);
            }
        });
    }

    function proceed(trigger) {
        if (trigger.tagName === 'A') {
            window.location.href = trigger.href;
            return;
        }
        trigger.dataset.confirmed = 'yes';
        trigger.click();
        delete trigger.dataset.confirmed;
    }

    /* ---------- loading state while a POST form submits ---------- */
    document.addEventListener('submit', function (e) {
        var form = e.target;
        if (e.defaultPrevented || form.method.toLowerCase() !== 'post' || form.hasAttribute('data-no-loading')) {
            return;
        }
        var button = e.submitter || form.querySelector('[type="submit"]');
        if (!button || button.classList.contains('is-loading')) {
            return;
        }
        // let the browser serialise the clicked button first, then lock it
        window.setTimeout(function () {
            button.classList.add('is-loading');
            button.setAttribute('aria-busy', 'true');
            button.disabled = true;
            var spinner = document.createElement('span');
            spinner.className = 'spinner';
            spinner.setAttribute('aria-hidden', 'true');
            button.prepend(spinner);
        }, 0);
    }, false);

    // back/forward cache restores a page with locked buttons; unlock them
    window.addEventListener('pageshow', function (e) {
        if (!e.persisted) {
            return;
        }
        document.querySelectorAll('.btn.is-loading').forEach(function (b) {
            b.classList.remove('is-loading');
            b.removeAttribute('aria-busy');
            b.disabled = false;
            var s = b.querySelector('.spinner');
            if (s) {
                s.remove();
            }
        });
    });

    /* ---------- responsive tables: copy header text into each cell ---------- */
    document.querySelectorAll('table.table-cards').forEach(function (table) {
        var headers = Array.prototype.map.call(table.querySelectorAll('thead th'), function (th) {
            return th.textContent.trim();
        });
        table.querySelectorAll('tbody tr').forEach(function (tr) {
            Array.prototype.forEach.call(tr.children, function (td, i) {
                if (!td.hasAttribute('data-label') && headers[i] !== undefined) {
                    td.setAttribute('data-label', headers[i]);
                }
            });
        });
    });

    /* ---------- client-side list filtering ---------- */
    var filterTargets = {};
    document.querySelectorAll('[data-filter-target]').forEach(function (control) {
        var target = control.getAttribute('data-filter-target');
        (filterTargets[target] = filterTargets[target] || []).push(control);
        control.addEventListener(control.tagName === 'SELECT' ? 'change' : 'input', function () {
            applyFilter(target);
        });
    });
    Object.keys(filterTargets).forEach(applyFilter);

    function applyFilter(target) {
        var table = document.querySelector(target);
        if (!table) {
            return;
        }
        var controls = filterTargets[target];
        // any element with data-row: table rows, list items, cards
        var rows = table.querySelectorAll('[data-row]');
        var shown = 0;
        rows.forEach(function (tr) {
            var visible = controls.every(function (c) {
                var value = c.value.trim().toLowerCase();
                if (!value) {
                    return true;
                }
                var key = c.getAttribute('data-filter-key');
                if (key) {
                    // the row attribute may hold several space-separated tokens, e.g. data-status="unpaid overdue"
                    return (tr.getAttribute('data-' + key) || '').toLowerCase().split(/\s+/).indexOf(value) !== -1;
                }
                return tr.textContent.toLowerCase().indexOf(value) !== -1;
            });
            tr.hidden = !visible;
            if (visible) {
                shown++;
            }
        });
        document.querySelectorAll('[data-filter-count="' + target + '"]').forEach(function (el) {
            el.textContent = shown + ' รายการ';
        });
        document.querySelectorAll('[data-filter-empty="' + target + '"]').forEach(function (el) {
            el.hidden = shown !== 0 || rows.length === 0;
        });
    }
})();
