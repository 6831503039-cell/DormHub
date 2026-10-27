/*
 * Viewer preferences, loaded in <head> of every page so the theme is applied before the page paints.
 *  Theme:    "light" (default), "dark" or "system", saved in localStorage by the Settings dialog (ui.js)
 *  Language: chosen on the server (?lang=th|en, kept in the "lang" cookie); <html lang> tells scripts which one.
 *            dhT('ไทย', 'English') returns the text for the page language.
 */
(function () {
    'use strict';
    var KEY = 'dh-theme';
    var media = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;

    function saved() {
        try {
            return localStorage.getItem(KEY) || 'light';
        } catch (e) {
            return 'light';
        }
    }

    function apply(choice) {
        var dark = choice === 'dark' || (choice === 'system' && media !== null && media.matches);
        document.documentElement.setAttribute('data-bs-theme', dark ? 'dark' : 'light');
    }

    window.dhTheme = {
        get: saved,
        set: function (choice) {
            try {
                localStorage.setItem(KEY, choice);
            } catch (e) {
                // private mode: the choice still applies to this page
            }
            apply(choice);
        }
    };
    window.dhT = function (th, en) {
        return document.documentElement.lang === 'en' ? en : th;
    };

    apply(saved());
    if (media !== null && media.addEventListener) {
        media.addEventListener('change', function () {
            if (saved() === 'system') {
                apply('system');
            }
        });
    }
})();
