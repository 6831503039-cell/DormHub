/* Eye button inside a password box: shows or hides the password (login and register pages). */
document.querySelectorAll('[data-toggle-password]').forEach(function (btn) {
    var input = document.getElementById(btn.getAttribute('data-toggle-password'));
    btn.addEventListener('click', function () {
        var show = input.type === 'password';
        input.type = show ? 'text' : 'password';
        btn.setAttribute('aria-pressed', show);
        btn.setAttribute('aria-label', show ? 'ซ่อนรหัสผ่าน' : 'แสดงรหัสผ่าน');
        btn.querySelector('.when-hidden').hidden = show;
        btn.querySelector('.when-shown').hidden = !show;
        input.focus();
    });
});
