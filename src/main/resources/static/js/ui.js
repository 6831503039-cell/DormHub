/*
 * DormHub shared UI behaviour (design system). Loaded once by every layout.
 *
 *  data-confirm="ลบห้อง A101?"        on <a> or a submit <button>: asks in the confirm modal first
 *      data-confirm-title, data-confirm-ok (button label, default "ลบ" / "Delete"),
 *      data-confirm-tone="primary" for a non-destructive confirmation (default "danger")
 *  POST forms                          the submit button shows a spinner and is disabled while sending
 *      add data-no-loading to a form to opt out
 *  <table class="table-cards">         each cell gets data-label from its column header, so the row
 *                                      can be shown as a stacked card on phones (see app.css)
 *  data-filter-target="#listId"        on a search <input>: hides [data-row] elements inside #listId
 *                                      (table rows or list items) that do not contain the text
 *  data-filter-key="status"            on a <select> with data-filter-target: keeps rows whose
 *                                      data-status contains the value as a token ("" = all)
 *  <details data-group> inside the target: a collapsible group of rows (one per building); filtering
 *                                      opens the groups with matches and hides the empty ones
 *  data-filter-count="#tableId"        element that shows "N items"
 *  data-filter-empty="#tableId"        element shown when no row matches
 *  <select data-room-picker>          room options with data-building / data-floor: shows building tabs
 *                                      above the select and only that building's rooms, grouped by floor
 *  #settingsModal                     Settings dialog (fragments/ui :: settingsModal): language links
 *                                      keep the current page, theme radios apply at once (prefs.js)
 * Texts in scripts use dhT('ไทย', 'English') from prefs.js.
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
        modalEl.querySelector('[data-confirm-title]').textContent = trigger.getAttribute('data-confirm-title') || dhT('ยืนยันการลบ', 'Confirm delete');
        modalEl.querySelector('[data-confirm-message]').textContent = message;
        var okButton = modalEl.querySelector('[data-confirm-ok]');
        okButton.textContent = trigger.getAttribute('data-confirm-ok') || dhT('ลบ', 'Delete');
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
        // collapsible groups (<details data-group>, e.g. one per building): while searching or filtering, groups with
        // matches open and the others hide; when every filter is cleared they close again
        var filtering = controls.some(function (c) { return c.value.trim() !== ''; });
        table.querySelectorAll('details[data-group]').forEach(function (g) {
            var matches = g.querySelectorAll('[data-row]:not([hidden])').length;
            g.hidden = matches === 0 && rows.length > 0;
            if (filtering) {
                g.open = matches > 0;
            } else if (table.dataset.wasFiltering === 'yes') {
                g.open = false;
            }
        });
        table.dataset.wasFiltering = filtering ? 'yes' : 'no';
        document.querySelectorAll('[data-filter-count="' + target + '"]').forEach(function (el) {
            el.textContent = shown + dhT(' รายการ', shown === 1 ? ' item' : ' items');
        });
        document.querySelectorAll('[data-filter-empty="' + target + '"]').forEach(function (el) {
            el.hidden = shown !== 0 || rows.length === 0;
        });
    }

    /* ---------- room picker: building tabs above a room <select>, rooms grouped by floor ---------- */
    document.querySelectorAll('select[data-room-picker]').forEach(function (select) {
        var empty = select.querySelector('option[value=""]');
        var rooms = Array.prototype.filter.call(select.options, function (o) { return o.value !== ''; });
        var buildings = [];
        rooms.forEach(function (o) {
            if (o.dataset.building && buildings.indexOf(o.dataset.building) === -1) {
                buildings.push(o.dataset.building);
            }
        });
        if (buildings.length < 2) {
            return;
        }
        buildings.sort();
        var picked = rooms.filter(function (o) { return o.selected; })[0];
        var current = picked ? picked.dataset.building : buildings[0];

        var tabs = document.createElement('div');
        tabs.className = 'room-picker-tabs';
        tabs.setAttribute('role', 'group');
        tabs.setAttribute('aria-label', dhT('เลือกตึก', 'Choose building'));
        buildings.forEach(function (b) {
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'room-picker-tab';
            btn.dataset.building = b;
            btn.textContent = dhT('ตึก ', 'Building ') + b;
            btn.addEventListener('click', function () {
                current = b;
                render(true);
                select.focus();
            });
            tabs.appendChild(btn);
        });
        select.parentNode.insertBefore(tabs, select);

        function render(notify) {
            var keep = select.value;
            select.innerHTML = '';
            if (empty) {
                select.appendChild(empty);
            }
            var floors = {};
            rooms.forEach(function (o) {
                if (o.dataset.building !== current) {
                    return;
                }
                var f = o.dataset.floor || '';
                if (!floors[f]) {
                    floors[f] = document.createElement('optgroup');
                    floors[f].label = f ? dhT('ชั้น ', 'Floor ') + f : '';
                    select.appendChild(floors[f]);
                }
                floors[f].appendChild(o);
            });
            // a room of another building stays chosen only while its building is shown
            select.value = rooms.some(function (o) { return o.value === keep && o.dataset.building === current; }) ? keep : '';
            tabs.querySelectorAll('.room-picker-tab').forEach(function (t) {
                var on = t.dataset.building === current;
                t.classList.toggle('active', on);
                t.setAttribute('aria-pressed', on);
            });
            if (notify) {
                select.dispatchEvent(new Event('change', { bubbles: true }));
            }
        }
        render();
    });

    /* ---------- Settings dialog: language and theme ---------- */
    var settings = document.getElementById('settingsModal');
    if (settings) {
        // the language links reload the current page with ?lang=..., keeping its other query parameters
        settings.querySelectorAll('[data-set-lang]').forEach(function (link) {
            var url = new URL(window.location.href);
            url.searchParams.set('lang', link.getAttribute('data-set-lang'));
            link.href = url.pathname + url.search + url.hash;
        });
        var current = window.dhTheme ? window.dhTheme.get() : 'light';
        settings.querySelectorAll('input[name="theme"]').forEach(function (radio) {
            radio.checked = radio.value === current;
            radio.addEventListener('change', function () {
                if (radio.checked && window.dhTheme) {
                    window.dhTheme.set(radio.value);
                }
            });
        });
    }

    // after switching language, drop ?lang= from the address so a reload or shared link stays clean
    if (window.history.replaceState && /[?&]lang=/.test(window.location.search)) {
        var clean = new URL(window.location.href);
        clean.searchParams.delete('lang');
        window.history.replaceState(window.history.state, '', clean.pathname + clean.search + clean.hash);
    }
})();
