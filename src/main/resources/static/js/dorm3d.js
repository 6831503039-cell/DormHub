/*
 * DormHub 3D room map (Three.js r128 + OrbitControls, loaded from CDN before this file).
 *
 *   Dorm3D.mount({
 *     root: element,            // container with a fixed height
 *     dataUrl: '/explore/rooms',// JSON list of RoomView (see th.mfu.service.RoomView)
 *     mode: 'public' | 'admin',
 *     soonDays: 60,
 *     editUrl: '/admin/rooms/', // admin: link to the room form (+ id)
 *     bookingsUrl: '/admin/bookings',
 *     bookTemplate: element,    // public: <template> holding the booking form
 *     initialRoom: 'A103'       // optional room number to open first
 *   });
 *
 * Views: site (all buildings) -> building (rooms coloured by status) -> floor (upper floors lift away)
 *        -> room sheet (details, booking) -> room 3D (furnished model built from the room type).
 */
(function () {
    'use strict';

    var FH = 3.3;          // floor height
    var ROOM_W = 4.6;      // room width along the building
    var DEPTH = 10;        // building depth
    var CORRIDOR = 1.8;

    var COLORS = {
        ground: 0xeef2f7, lawn: 0xd6e8cf, path: 0xffffff, water: 0x9fd3e6,
        slab: 0xffffff, glass: 0xb9d4f2, edge: 0x8a96a8, neutral: 0xe6ebf2,
        tree: 0x8fbf7f, trunk: 0x9b8468,
        AVAILABLE: 0x22a565, SOON: 0xf2994a, OCCUPIED: 0x2f80ed, MAINTENANCE: 0xf5b301,
        OCCUPIED_PUBLIC: 0xbcc5d2, select: 0x2f80ed
    };
    var CSS_COLORS = { AVAILABLE: '#22a565', SOON: '#f2994a', OCCUPIED: '#2f80ed', MAINTENANCE: '#f5b301', OCCUPIED_PUBLIC: '#aab4c3' };

    function el(tag, cls, html) {
        var e = document.createElement(tag);
        if (cls) e.className = cls;
        if (html != null) e.innerHTML = html;
        return e;
    }
    function esc(s) {
        return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
            return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
        });
    }
    function baht(n) { return Number(n || 0).toLocaleString('th-TH', { maximumFractionDigits: 0 }); }
    function thDate(iso) {
        if (!iso) return '-';
        var p = iso.split('-');
        return new Date(+p[0], +p[1] - 1, +p[2]).toLocaleDateString('th-TH', { day: 'numeric', month: 'short', year: 'numeric' });
    }
    function damp(k, dt) { return 1 - Math.exp(-k * dt); }
    function easeInOut(t) { return t < .5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2; }
    var reduceMotion = window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches;

    function mount(opts) {
        var root = opts.root;
        var isAdmin = opts.mode === 'admin';
        var soonDays = opts.soonDays || 60;
        root.classList.add('d3-root');

        if (!window.THREE || !THREE.OrbitControls) {
            root.innerHTML = '<div class="d3-msg">โหลดไลบรารี 3D ไม่สำเร็จ ตรวจสอบการเชื่อมต่ออินเทอร์เน็ตแล้วลองใหม่</div>';
            return;
        }

        // ---------- DOM ----------
        var host = el('div', 'd3-canvas');
        var labelsEl = el('div', 'd3-labels');
        var tip = el('div', 'd3-tip'); tip.hidden = true;
        var panel = el('aside', 'd3-panel');
        var crumbs = el('nav', 'd3-crumbs'); crumbs.setAttribute('aria-label', 'ตำแหน่ง');
        var pbody = el('div', 'd3-pbody');
        panel.appendChild(crumbs); panel.appendChild(pbody);
        var sheet = el('section', 'd3-sheet closed'); sheet.setAttribute('aria-live', 'polite');
        var shead = el('div', 'd3-shead');
        var closeBtn = el('button', 'btn btn-light btn-icon d3-x', '&times;'); closeBtn.setAttribute('aria-label', 'ปิด');
        var sheadWrap = el('div', 'd3-shead-wrap'); sheadWrap.appendChild(shead); sheadWrap.appendChild(closeBtn);
        var sbody = el('div', 'd3-sbody');
        sheet.appendChild(sheadWrap); sheet.appendChild(sbody);
        var loading = el('div', 'd3-msg', '<div class="spinner-border text-primary mb-2" role="status"></div><div>กำลังโหลดแผนผังหอพัก…</div>');
        var roomView = el('div', 'd3-room3d'); roomView.hidden = true;
        [host, labelsEl, tip, panel, sheet, roomView, loading].forEach(function (n) { root.appendChild(n); });

        // ---------- three.js ----------
        var renderer;
        try {
            renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
        } catch (e) {
            loading.innerHTML = 'เบราว์เซอร์นี้แสดงภาพ 3 มิติไม่ได้ ลองเปิดด้วย Chrome หรือ Safari รุ่นใหม่';
            return;
        }
        renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
        renderer.shadowMap.enabled = true;
        renderer.shadowMap.type = THREE.PCFSoftShadowMap;
        host.appendChild(renderer.domElement);

        var scene = new THREE.Scene();
        var camera = new THREE.PerspectiveCamera(38, 1, 0.5, 700);
        var controls = new THREE.OrbitControls(camera, renderer.domElement);
        controls.enableDamping = true;
        controls.dampingFactor = 0.07;
        controls.rotateSpeed = 0.6;
        controls.zoomSpeed = 0.8;
        controls.maxPolarAngle = 1.36;
        controls.minDistance = 10;
        controls.maxDistance = 170;

        scene.add(new THREE.HemisphereLight(0xffffff, 0xb8c2d0, 0.8));
        var sun = new THREE.DirectionalLight(0xffffff, 0.75);
        sun.position.set(-35, 70, 45);
        sun.castShadow = true;
        sun.shadow.mapSize.set(2048, 2048);
        sun.shadow.camera.left = -60; sun.shadow.camera.right = 60;
        sun.shadow.camera.top = 60; sun.shadow.camera.bottom = -60;
        sun.shadow.camera.far = 200;
        sun.shadow.bias = -0.0005;
        scene.add(sun);

        function std(color, extra) {
            var o = { color: color, roughness: 0.85, metalness: 0 };
            for (var k in extra) o[k] = extra[k];
            return new THREE.MeshStandardMaterial(o);
        }

        var state = { view: 'site', b: null, f: null, room: null, onlyBookable: !isAdmin };
        var buildings = [];      // { id, cx, cz, L, axis, front, floors:[{n, group, mats, y, op, rooms}], ... }
        var allRooms = [];       // RoomView + { mesh, pos, building }
        var roomMeshes = [];
        var labels = [];

        // ---------- data ----------
        fetch(opts.dataUrl, { credentials: 'same-origin', headers: { 'Accept': 'application/json' } })
            .then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); })
            .then(function (data) { build(data); loading.remove(); start(); })
            .catch(function () {
                loading.innerHTML = 'โหลดข้อมูลห้องไม่สำเร็จ <a href="">ลองใหม่</a>';
            });

        function statusColor(s) {
            if (s === 'OCCUPIED' && !isAdmin) return COLORS.OCCUPIED_PUBLIC;
            return COLORS[s] || COLORS.neutral;
        }
        function statusCss(s) {
            if (s === 'OCCUPIED' && !isAdmin) return CSS_COLORS.OCCUPIED_PUBLIC;
            return CSS_COLORS[s] || '#9aa4b2';
        }
        function bookable(r) { return r.status === 'AVAILABLE' || r.status === 'SOON'; }

        // world position helpers: u along the building, v across (v > 0 = courtyard side)
        function toWorld(b, u, v) { return b.axis === 'x' ? [b.cx + u, b.cz + v * b.front] : [b.cx + v * b.front, b.cz + u]; }
        function dims(b, lu, lv) { return b.axis === 'x' ? [lu, lv] : [lv, lu]; }

        function build(data) {
            // group: building -> floor -> rooms (already sorted by room number)
            var map = {};
            data.forEach(function (r) {
                (map[r.building] = map[r.building] || {});
                (map[r.building][r.floor] = map[r.building][r.floor] || []).push(r);
            });
            var ids = Object.keys(map).sort();
            ids.forEach(function (id) {
                var floorsMap = map[id];
                var maxRooms = 0, maxFloor = 0;
                Object.keys(floorsMap).forEach(function (f) {
                    maxRooms = Math.max(maxRooms, floorsMap[f].length);
                    maxFloor = Math.max(maxFloor, +f);
                });
                var cols = Math.max(1, Math.ceil(maxRooms / 2));
                buildings.push({ id: id, floorsMap: floorsMap, cols: cols, L: cols * ROOM_W, floorCount: maxFloor });
            });

            // U-shaped site like the dorm sketch: A at the top, C on the left, B on the right
            var A = find('A');
            var topZ = -16;
            var aLen = A ? A.L : 14;
            var sideX = aLen / 2 + DEPTH / 2 + 2.5;
            var extra = 0;
            buildings.forEach(function (b) {
                if (b.id === 'A') { b.axis = 'x'; b.front = 1; b.cx = 0; b.cz = topZ; }
                else if (b.id === 'B') { b.axis = 'z'; b.front = -1; b.cx = sideX; b.cz = topZ + DEPTH / 2 + b.L / 2 - 1; }
                else if (b.id === 'C') { b.axis = 'z'; b.front = 1; b.cx = -sideX; b.cz = topZ + DEPTH / 2 + b.L / 2 - 1; }
                else { b.axis = 'x'; b.front = -1; b.cx = (extra - 0.5) * (b.L + 6); b.cz = 30; extra++; }
            });

            buildSite(sideX, topZ);
            buildings.forEach(buildBuilding);
            refreshColors(true);
        }
        function find(id) { for (var i = 0; i < buildings.length; i++) if (buildings[i].id === id) return buildings[i]; return null; }

        function buildSite(sideX, topZ) {
            var ground = new THREE.Mesh(new THREE.PlaneGeometry(300, 300), std(COLORS.ground, { roughness: 1 }));
            ground.rotation.x = -Math.PI / 2;
            ground.receiveShadow = true;
            scene.add(ground);

            var cw = sideX * 2 - DEPTH - 3, cz0 = topZ + DEPTH / 2 + 1, cd = 26;
            var lawn = new THREE.Mesh(new THREE.PlaneGeometry(cw, cd), std(COLORS.lawn, { roughness: 1 }));
            lawn.rotation.x = -Math.PI / 2;
            lawn.position.set(0, 0.02, cz0 + cd / 2);
            lawn.receiveShadow = true;
            scene.add(lawn);

            var deck = new THREE.Mesh(new THREE.BoxGeometry(Math.min(12, cw - 4), 0.14, 7), std(COLORS.path));
            deck.position.set(0, 0.07, cz0 + 9);
            deck.receiveShadow = true;
            scene.add(deck);
            var pool = new THREE.Mesh(new THREE.BoxGeometry(Math.min(9, cw - 7), 0.2, 4), std(COLORS.water, { roughness: 0.15 }));
            pool.position.set(0, 0.12, cz0 + 9);
            scene.add(pool);
            var path = new THREE.Mesh(new THREE.PlaneGeometry(3.2, cd - 12), std(COLORS.path, { roughness: 1 }));
            path.rotation.x = -Math.PI / 2;
            path.position.set(0, 0.03, cz0 + 12.5 + (cd - 12) / 2);
            path.receiveShadow = true;
            scene.add(path);

            var trunkM = std(COLORS.trunk), leafM = std(COLORS.tree, { flatShading: true });
            var spots = [[-0.38, 0.12], [0.38, 0.1], [-0.4, 0.55], [0.4, 0.5], [-0.3, 0.85], [0.3, 0.88], [-0.12, 0.3], [0.15, 0.62]];
            spots.forEach(function (s, k) {
                var x = s[0] * cw, z = cz0 + s[1] * cd, h = 2.3 + (k % 3) * 0.5;
                var t = new THREE.Mesh(new THREE.CylinderGeometry(0.12, 0.16, h, 6), trunkM);
                t.position.set(x, h / 2, z); t.castShadow = true; scene.add(t);
                var c = new THREE.Mesh(new THREE.IcosahedronGeometry(1.2 + (k % 2) * 0.4, 0), leafM);
                c.position.set(x, h + 0.6, z); c.castShadow = true; scene.add(c);
            });

            var tag = el('span', 'd3-lbl d3-lbl-site', 'ส่วนกลาง');
            addLabel(tag, new THREE.Vector3(0, 0.5, cz0 + 4), function () { return state.view === 'site'; }, 'center');
        }

        function buildBuilding(b) {
            b.group = new THREE.Group();
            b.op = 1; b.opTarget = 1;
            b.floors = [];
            var rd = (DEPTH - CORRIDOR) / 2;
            var c = toWorld(b, 0, 0);
            b.center = new THREE.Vector3(c[0], 0, c[1]);

            for (var f = 1; f <= b.floorCount; f++) {
                var g = new THREE.Group();
                var mats = [];
                var y0 = (f - 1) * FH;
                var slabM = std(COLORS.slab, { transparent: true }); mats.push(slabM);
                var glassM = std(COLORS.glass, { transparent: true, opacity: 0.16, roughness: 0.1, depthWrite: false }); glassM.userData.base = 0.16; mats.push(glassM);
                var edgeM = new THREE.LineBasicMaterial({ color: COLORS.edge, transparent: true, opacity: 0.45 }); edgeM.userData.base = 0.45; mats.push(edgeM);

                var sd = dims(b, b.L + 0.6, DEPTH + 0.6);
                var slab = new THREE.Mesh(new THREE.BoxGeometry(sd[0], 0.35, sd[1]), slabM);
                slab.position.set(c[0], y0 + 0.175, c[1]);
                slab.castShadow = slab.receiveShadow = true;
                g.add(slab);

                var gd = dims(b, b.L, DEPTH);
                var shell = new THREE.Mesh(new THREE.BoxGeometry(gd[0], FH - 0.35, gd[1]), glassM);
                shell.position.set(c[0], y0 + 0.35 + (FH - 0.35) / 2, c[1]);
                shell.renderOrder = 2;
                g.add(shell);
                var edges = new THREE.LineSegments(new THREE.EdgesGeometry(shell.geometry), edgeM);
                edges.position.copy(shell.position);
                g.add(edges);

                var list = b.floorsMap[f] || [];
                list.forEach(function (r, j) {
                    var side = j < b.cols ? 0 : 1;
                    var i = j % b.cols;
                    var u = -b.L / 2 + ROOM_W * (i + 0.5);
                    var v = (side === 0 ? 1 : -1) * (CORRIDOR / 2 + rd / 2);
                    var p = toWorld(b, u, v);
                    var d = dims(b, ROOM_W - 0.24, rd - 0.24);
                    var geo = new THREE.BoxGeometry(d[0], 2.55, d[1]);
                    geo.translate(0, 1.275, 0);
                    var m = std(COLORS.neutral, { transparent: true, roughness: 0.7 });
                    var mesh = new THREE.Mesh(geo, m);
                    mesh.position.set(p[0], y0 + 0.35, p[1]);
                    mesh.castShadow = mesh.receiveShadow = true;
                    mesh.userData.room = r;
                    r.mesh = mesh;
                    r.side = side;
                    r.target = new THREE.Color(COLORS.neutral);
                    r.hover = 0;
                    r.pos = new THREE.Vector3(p[0], y0 + 3.1, p[1]);
                    r.b = b;
                    g.add(mesh);
                    roomMeshes.push(mesh);
                    allRooms.push(r);
                    if (r.status !== 'MAINTENANCE' || isAdmin) {
                        var tag = el('span', 'd3-lbl d3-lbl-room', esc(r.number));
                        r.tag = tag;
                        addLabel(tag, r.pos, (function (rr, ff) {
                            return function () { return state.view === 'floor' && state.b === b.id && state.f === ff; };
                        })(r, f), 'center');
                    }
                });

                g.userData = { n: f, y: 0, yTarget: 0, op: 1, opTarget: 1 };
                b.floors.push({ n: f, group: g, mats: mats, rooms: list });
                b.group.add(g);

                // clickable floor chip
                var chip = el('button', 'd3-lbl d3-lbl-floor', 'ชั้น ' + f);
                chip.type = 'button';
                chip.addEventListener('click', (function (ff) { return function () { goFloor(ff); }; })(f));
                var endPos = b.axis === 'x'
                    ? new THREE.Vector3(b.cx + b.L / 2 + 1.1, y0 + 1.7, b.cz + (DEPTH / 2) * b.front)
                    : new THREE.Vector3(b.cx + (DEPTH / 2) * b.front, y0 + 1.7, b.cz + b.L / 2 + 1.1);
                (function (ff, chipEl) {
                    var lb = addLabel(chipEl, endPos, function () {
                        return state.view !== 'site' && state.b === b.id && (state.f == null || ff <= state.f);
                    }, 'left');
                    lb.after = function () { chipEl.classList.toggle('active', state.f === ff); };
                })(f, chip);
            }

            var rdm = dims(b, b.L + 0.6, DEPTH + 0.6);
            b.roofMat = std(COLORS.slab, { transparent: true });
            var roof = new THREE.Mesh(new THREE.BoxGeometry(rdm[0], 0.4, rdm[1]), b.roofMat);
            roof.position.set(c[0], b.floorCount * FH + 0.2, c[1]);
            roof.castShadow = true;
            b.roof = roof;
            b.roofY = 0; b.roofOp = 1;
            b.group.add(roof);
            b.group.traverse(function (o) { o.userData.building = b.id; });
            scene.add(b.group);

            var tag = el('button', 'd3-lbl d3-lbl-building');
            tag.type = 'button';
            tag.addEventListener('click', function () { goBuilding(b.id); });
            b.tag = tag;
            addLabel(tag, b.center.clone().setY(b.floorCount * FH + 3), function () { return state.view === 'site'; }, 'top');
        }

        // ---------- labels ----------
        function addLabel(node, pos, show, anchor) {
            labelsEl.appendChild(node);
            var l = { el: node, pos: pos, show: show, anchor: anchor, on: false };
            labels.push(l);
            return l;
        }
        function updateLabels() {
            labels.forEach(function (l) {
                l.on = !!l.show();
                l.el.classList.toggle('on', l.on);
                if (l.el.tagName === 'BUTTON') l.el.tabIndex = l.on ? 0 : -1;
                if (l.after) l.after();
            });
        }
        var v3 = new THREE.Vector3();
        function placeLabels() {
            var w = host.clientWidth, h = host.clientHeight;
            for (var i = 0; i < labels.length; i++) {
                var l = labels[i];
                if (!l.on) continue;
                v3.copy(l.pos);
                if (l.el._floor) v3.y += l.el._floor.group.position.y;
                v3.project(camera);
                var x = (v3.x * 0.5 + 0.5) * w, y = (-v3.y * 0.5 + 0.5) * h;
                var t = l.anchor === 'left' ? 'translate(0,-50%)' : l.anchor === 'center' ? 'translate(-50%,-50%)' : 'translate(-50%,-100%)';
                l.el.style.transform = 'translate(' + x.toFixed(1) + 'px,' + y.toFixed(1) + 'px) ' + t;
                l.el.style.visibility = v3.z > 1 ? 'hidden' : '';
            }
        }

        // ---------- colours / visibility targets ----------
        function refreshColors(instant) {
            buildings.forEach(function (b) {
                var active = state.b === b.id;
                b.opTarget = state.b && !active ? 0.1 : 1;
                b.floors.forEach(function (fl) {
                    var above = active && state.f != null && fl.n > state.f;
                    fl.group.userData.yTarget = above ? 6 + (fl.n - state.f) * 2.5 : 0;
                    fl.group.userData.opTarget = above ? 0 : 1;
                });
                b.roofTargetY = active && state.f != null ? 6 + (b.floorCount + 1 - state.f) * 2.5 : 0;
                b.roofTargetOp = active && state.f != null ? 0 : 1;
                b.tag.innerHTML = 'ตึก ' + esc(b.id) + '<small>' + countText(roomsOf(b)) + '</small>';
            });
            allRooms.forEach(function (r) {
                var active = state.b === r.b.id;
                var c = COLORS.neutral;
                if (active || state.view === 'site') c = statusColor(r.status);
                if (state.view === 'site') c = bookable(r) ? statusColor(r.status) : COLORS.neutral;
                if (active && state.onlyBookable && !bookable(r)) c = COLORS.neutral;
                r.target.setHex(c);
                if (instant) r.mesh.material.color.copy(r.target);
            });
            updateLabels();
        }
        function roomsOf(b, floor) {
            return allRooms.filter(function (r) { return r.b === b && (floor == null || r.floor === floor); });
        }
        function counts(list) {
            var c = { AVAILABLE: 0, SOON: 0, OCCUPIED: 0, MAINTENANCE: 0, total: list.length };
            list.forEach(function (r) { c[r.status] = (c[r.status] || 0) + 1; });
            return c;
        }
        function countText(list) {
            var c = counts(list);
            return 'ว่าง ' + c.AVAILABLE + ' · ใกล้ว่าง ' + c.SOON;
        }

        // ---------- camera ----------
        var tween = null;
        function flyTo(pos, target, dur) {
            tween = { p0: camera.position.clone(), t0: controls.target.clone(), p1: pos, t1: target, s: performance.now(), d: reduceMotion ? 1 : (dur || 1100) };
        }
        function narrow() { return root.clientWidth < 760; }
        function siteView() {
            return narrow()
                ? [new THREE.Vector3(-18, 80, 84), new THREE.Vector3(0, 0, 4)]
                : [new THREE.Vector3(-32, 48, 46), new THREE.Vector3(-7, 5, -2)];
        }
        function buildingView(b, floor) {
            var out = b.axis === 'x' ? new THREE.Vector3(0, 0, b.front) : new THREE.Vector3(b.front, 0, 0);
            var side = b.axis === 'x' ? new THREE.Vector3(1, 0, 0) : new THREE.Vector3(0, 0, 1);
            var far = narrow() ? 1.45 : 1;
            var t, p;
            if (floor == null) {
                t = b.center.clone().setY(b.floorCount * FH / 2);
                p = t.clone().addScaledVector(out, (24 + b.L * 1.3) * far).addScaledVector(side, -9).setY(t.y + 14 * far);
            } else {
                t = b.center.clone().setY((floor - 1) * FH + 1.4);
                p = t.clone().addScaledVector(out, 12 * far).addScaledVector(side, -3);
                p.y += (12 + b.L * 0.9) * far;
            }
            return [p, t];
        }

        // ---------- navigation ----------
        function goSite() {
            state.view = 'site'; state.b = null; state.f = null; closeSheet();
            var v = siteView(); flyTo(v[0], v[1]); refreshColors(); renderPanel();
        }
        function goBuilding(id) {
            state.view = 'building'; state.b = id; state.f = null; closeSheet();
            var v = buildingView(find(id)); flyTo(v[0], v[1]); refreshColors(); renderPanel();
        }
        function goFloor(f) {
            state.view = 'floor'; state.f = f;
            if (state.room && state.room.floor !== f) closeSheet();
            var v = buildingView(find(state.b), f); flyTo(v[0], v[1]); refreshColors(); renderPanel();
        }

        // ---------- side panel ----------
        function dot(color) { return '<i class="d3-dot" style="background:' + color + '"></i>'; }
        function legend() {
            return '<div class="d3-legend">' +
                '<span>' + dot(statusCss('AVAILABLE')) + 'ว่าง</span>' +
                '<span>' + dot(statusCss('SOON')) + 'ใกล้หมดสัญญา (≤ ' + soonDays + ' วัน)</span>' +
                '<span>' + dot(statusCss('OCCUPIED')) + 'มีผู้เช่า</span>' +
                '<span>' + dot(statusCss('MAINTENANCE')) + 'ปิดซ่อม</span></div>';
        }
        function bar(c) {
            var t = c.total || 1;
            return '<span class="d3-bar">' +
                '<i style="width:' + (c.AVAILABLE / t * 100) + '%;background:' + statusCss('AVAILABLE') + '"></i>' +
                '<i style="width:' + (c.SOON / t * 100) + '%;background:' + statusCss('SOON') + '"></i>' +
                '<i style="width:' + (c.OCCUPIED / t * 100) + '%;background:' + statusCss('OCCUPIED') + '"></i>' +
                '<i style="width:' + (c.MAINTENANCE / t * 100) + '%;background:' + statusCss('MAINTENANCE') + '"></i></span>';
        }
        function renderPanel() {
            var parts = ['<button type="button" data-go="site">ภาพรวม</button>'];
            if (state.b) parts.push('<span>›</span>', state.f ? '<button type="button" data-go="b">ตึก ' + esc(state.b) + '</button>' : '<span>ตึก ' + esc(state.b) + '</span>');
            if (state.f) parts.push('<span>›</span><span>ชั้น ' + state.f + '</span>');
            crumbs.innerHTML = parts.join(' ');

            var html;
            if (state.view === 'site') {
                var all = counts(allRooms);
                html = '<div><h2 class="d3-h">เลือกตึก</h2><p class="d3-hint">ทั้งหมด ' + all.total + ' ห้อง ว่างตอนนี้ ' + all.AVAILABLE +
                    ' ห้อง และจะว่างภายใน ' + soonDays + ' วันอีก ' + all.SOON + ' ห้อง ลากเพื่อหมุน เลื่อนเพื่อซูม</p></div><div class="d3-list">';
                buildings.forEach(function (b) {
                    var c = counts(roomsOf(b));
                    html += '<button type="button" class="d3-bcard" data-b="' + esc(b.id) + '"><span class="d3-tag">' + esc(b.id) + '</span>' +
                        '<span><span class="fw-medium">ตึก ' + esc(b.id) + '</span><br><span class="small text-muted">' + b.floorCount + ' ชั้น · ' + c.total + ' ห้อง</span></span>' +
                        '<span class="d3-n">' + c.AVAILABLE + ' ว่าง<span>' + c.SOON + ' ใกล้ว่าง</span></span></button>';
                });
                html += '</div>' + legend();
            } else {
                var b = find(state.b);
                html = '<div><h2 class="d3-h">ตึก ' + esc(b.id) + (state.f ? ' ชั้น ' + state.f : '') + '</h2><p class="d3-hint">' +
                    (state.f ? 'กดที่ห้องเพื่อดูรายละเอียด' + (isAdmin ? '' : ' ราคา และจอง') : 'สีของแต่ละห้องบอกสถานะ เลือกชั้นเพื่อเปิดดูข้างใน') + '</p></div><div class="d3-floors">';
                for (var f = b.floorCount; f >= 1; f--) {
                    var c2 = counts(roomsOf(b, f));
                    html += '<button type="button" class="d3-fbtn' + (state.f === f ? ' active' : '') + '" data-f="' + f + '"><span class="fw-medium">ชั้น ' + f + '</span>' + bar(c2) +
                        '<span class="small text-muted">ว่าง ' + c2.AVAILABLE + '</span></button>';
                }
                html += '</div>';
                if (state.f) {
                    var list = roomsOf(b, state.f).filter(function (r) { return isAdmin || bookable(r); });
                    html += '<div class="d3-sec">' + (isAdmin ? 'ห้องในชั้นนี้' : 'ห้องที่จองได้') + '</div>';
                    html += list.length ? '<div class="d3-chips">' + list.map(function (r) {
                        return '<button type="button" class="d3-rbtn" data-r="' + esc(r.number) + '" style="border-color:' + statusCss(r.status) + '">' + esc(r.number) + '<i>฿' + baht(r.rent) + '</i></button>';
                    }).join('') + '</div>' : '<p class="d3-hint">ชั้นนี้ไม่มีห้องว่าง ลองดูชั้นอื่น</p>';
                }
                html += '<div class="d-flex flex-wrap gap-2"><button type="button" class="btn btn-sm ' + (state.onlyBookable ? 'btn-primary' : 'btn-light') + '" data-toggle="bookable" aria-pressed="' + state.onlyBookable + '">เน้นห้องที่จองได้</button>' +
                    (state.f ? '<button type="button" class="btn btn-sm btn-light" data-go="b">ดูทั้งตึก</button>' : '') + '</div>' + legend();
            }
            pbody.innerHTML = html;
        }
        panel.addEventListener('click', function (e) {
            var t = e.target.closest('button');
            if (!t) return;
            if (t.dataset.go === 'site') goSite();
            else if (t.dataset.go === 'b') goBuilding(state.b);
            else if (t.dataset.b) goBuilding(t.dataset.b);
            else if (t.dataset.f) goFloor(+t.dataset.f);
            else if (t.dataset.r) openRoom(byNumber(t.dataset.r));
            else if (t.dataset.toggle) { state.onlyBookable = !state.onlyBookable; refreshColors(); renderPanel(); }
        });
        function byNumber(n) { for (var i = 0; i < allRooms.length; i++) if (allRooms[i].number === n) return allRooms[i]; return null; }

        // ---------- room sheet ----------
        closeBtn.addEventListener('click', function () { closeSheet(); refreshColors(); });
        function closeSheet() { state.room = null; sheet.classList.add('closed'); }

        function openRoom(r, keepCamera) {
            if (!r) return;
            if (state.b !== r.b.id || state.f !== r.floor) {
                state.b = r.b.id; state.f = r.floor; state.view = 'floor';
                if (!keepCamera) { var v = buildingView(r.b, r.floor); flyTo(v[0], v[1]); }
            }
            state.room = r;
            refreshColors(); renderPanel();

            var big = /ใหญ่/.test(r.type || '') || r.capacity >= 3;
            shead.innerHTML = '<h3 class="d3-title">ห้อง ' + esc(r.number) + '</h3><div class="small text-muted">ตึก ' + esc(r.building) + ' · ชั้น ' + r.floor +
                ' · ' + (r.side === 0 ? 'ฝั่งส่วนกลาง' : 'ฝั่งด้านนอก') + '</div>';
            var statusLine;
            if (r.status === 'AVAILABLE') statusLine = 'ว่าง เข้าอยู่ได้ทันที';
            else if (r.status === 'SOON') statusLine = 'สัญญาปัจจุบันหมด <b>' + thDate(r.contractEnd) + '</b> จองล่วงหน้าได้ เข้าอยู่ได้ตั้งแต่ ' + thDate(r.availableFrom);
            else if (r.status === 'MAINTENANCE') statusLine = 'ห้องนี้ปิดซ่อมชั่วคราว';
            else statusLine = 'มีผู้เช่าแล้ว' + (r.contractEnd ? ' (สัญญาถึง ' + thDate(r.contractEnd) + ')' : '');

            var html = '<div class="d-flex align-items-center gap-2 flex-wrap"><span class="d3-pill" style="background:' + statusCss(r.status) + '">' + esc(r.statusLabel) + '</span>' +
                (r.pendingBookings ? '<span class="badge soft-warning">มีคำขอจอง ' + r.pendingBookings + ' รายการ</span>' : '') + '</div>' +
                '<button type="button" class="d3-preview" data-act="room3d"><span class="d3-preview-ic"><i data-lucide="box"></i></span><span><b>ดูห้องแบบ 3D</b><br><span class="small text-muted">หมุนดูการจัดห้อง ' + esc(r.type || '') + '</span></span><i data-lucide="chevron-right"></i></button>' +
                '<div class="d3-price"><b>฿' + baht(r.rent) + '</b><span>/ เดือน</span></div>' +
                '<div class="d3-status">' + statusLine + '</div>' +
                '<dl class="d3-spec">' +
                '<div><dt>ประเภท</dt><dd>' + esc(r.type || '-') + '</dd></div>' +
                '<div><dt>ขนาดโดยประมาณ</dt><dd>' + (big ? '32' : '24') + ' ตร.ม.</dd></div>' +
                '<div><dt>รับผู้เช่าได้</dt><dd>' + r.capacity + ' คน' + (r.capacity > 1 ? ' (หารค่าห้องได้)' : '') + '</dd></div>' +
                '<div><dt>' + (isAdmin ? 'ผู้เช่าตอนนี้' : 'ต่อคน (ถ้าอยู่เต็ม)') + '</dt><dd>' + (isAdmin ? r.occupants + ' / ' + r.capacity + ' คน' : '฿' + baht(r.rent / Math.max(1, r.capacity))) + '</dd></div>' +
                '</dl>';

            if (isAdmin) {
                html += '<div><div class="d3-sec mb-1">ผู้เช่า</div>' + ((r.tenants && r.tenants.length) ? '<ul class="d3-tenants">' + r.tenants.map(function (n) { return '<li>' + esc(n) + '</li>'; }).join('') + '</ul>' : '<div class="small text-muted">ยังไม่มีผู้เช่า</div>') + '</div>' +
                    '<div class="d-flex gap-2 flex-wrap"><a class="btn btn-primary" href="' + esc(opts.editUrl + r.id) + '"><i data-lucide="pencil"></i> แก้ไขห้อง</a>' +
                    (r.pendingBookings ? '<a class="btn btn-light" href="' + esc(opts.bookingsUrl) + '"><i data-lucide="inbox"></i> ดูคำขอจอง</a>' : '') + '</div>';
            } else if (bookable(r) && opts.bookTemplate) {
                html += '<div data-slot="book"></div>';
            } else {
                html += '<button class="btn btn-light w-100" disabled>' + (r.status === 'MAINTENANCE' ? 'ปิดซ่อม จองไม่ได้ชั่วคราว' : 'ห้องนี้มีผู้เช่าแล้ว') + '</button>';
            }
            sbody.innerHTML = html;

            var slot = sbody.querySelector('[data-slot="book"]');
            if (slot) {
                var form = opts.bookTemplate.content.firstElementChild.cloneNode(true);
                form.querySelector('[data-field="room"]').value = r.id;
                var date = form.querySelector('[data-field="date"]');
                date.min = r.availableFrom; date.value = r.availableFrom;
                form.querySelector('[data-field="title"]').textContent = r.status === 'SOON' ? 'จองล่วงหน้าห้อง ' + r.number : 'จองห้อง ' + r.number;
                slot.appendChild(form);
            }
            if (window.lucide) lucide.createIcons();
            sheet.classList.remove('closed');
            sbody.scrollTop = 0;
        }
        sbody.addEventListener('click', function (e) {
            var t = e.target.closest('[data-act="room3d"]');
            if (t && state.room) openRoom3D(state.room);
        });

        // ---------- picking ----------
        var ray = new THREE.Raycaster(), ndc = new THREE.Vector2(), down = null, hovered = null;
        function pick(e) {
            var rect = renderer.domElement.getBoundingClientRect();
            ndc.set(((e.clientX - rect.left) / rect.width) * 2 - 1, -((e.clientY - rect.top) / rect.height) * 2 + 1);
            ray.setFromCamera(ndc, camera);
            if (state.view === 'site') {
                var hit = ray.intersectObjects(buildings.map(function (b) { return b.group; }), true)[0];
                return hit ? { b: hit.object.userData.building } : null;
            }
            var cand = roomMeshes.filter(function (m) { var r = m.userData.room; return r.b.id === state.b && m.parent.userData.opTarget > 0; });
            var h2 = ray.intersectObjects(cand, false)[0];
            if (h2) return { room: h2.object.userData.room };
            var others = buildings.filter(function (b) { return b.id !== state.b; }).map(function (b) { return b.group; });
            var h3 = ray.intersectObjects(others, true)[0];
            return h3 ? { b: h3.object.userData.building } : null;
        }
        renderer.domElement.addEventListener('pointerdown', function (e) { down = { x: e.clientX, y: e.clientY }; tween = null; });
        renderer.domElement.addEventListener('pointerup', function (e) {
            if (!down) return;
            var moved = Math.hypot(e.clientX - down.x, e.clientY - down.y);
            down = null;
            if (moved > 6) return;
            var p = pick(e);
            if (!p) return;
            if (p.room) openRoom(p.room, state.view === 'floor');
            else if (p.b && p.b !== state.b) goBuilding(p.b);
        });
        renderer.domElement.addEventListener('pointermove', function (e) {
            if (e.pointerType !== 'mouse' || down) { tip.hidden = true; return; }
            var p = pick(e);
            if (hovered) hovered.hover = 0;
            hovered = p && p.room ? p.room : null;
            if (hovered) hovered.hover = 1;
            renderer.domElement.style.cursor = p ? 'pointer' : 'grab';
            if (p && p.room) tip.innerHTML = '<b>' + esc(p.room.number) + '</b> · ' + esc(p.room.statusLabel) + ' · ฿' + baht(p.room.rent);
            else if (p && p.b) tip.innerHTML = 'ตึก ' + esc(p.b) + ' · กดเพื่อเข้าไปดู';
            tip.hidden = !p;
            var rect = root.getBoundingClientRect();
            tip.style.transform = 'translate(' + (e.clientX - rect.left + 14) + 'px,' + (e.clientY - rect.top + 14) + 'px)';
        });
        renderer.domElement.addEventListener('pointerleave', function () { tip.hidden = true; if (hovered) hovered.hover = 0; hovered = null; });
        root.addEventListener('keydown', function (e) {
            if (e.key !== 'Escape') return;
            if (!roomView.hidden) closeRoom3D();
            else if (state.room) { closeSheet(); refreshColors(); }
            else if (state.f) goBuilding(state.b);
            else if (state.b) goSite();
        });

        // ---------- render loop ----------
        function resize() {
            var w = host.clientWidth, h = host.clientHeight;
            if (!w || !h) return;
            renderer.setSize(w, h, false);
            renderer.domElement.style.width = w + 'px';
            renderer.domElement.style.height = h + 'px';
            camera.aspect = w / h;
            camera.updateProjectionMatrix();
        }
        if (window.ResizeObserver) new ResizeObserver(resize).observe(host); else window.addEventListener('resize', resize);

        var last = performance.now();
        var pulse = 0;
        function loop(now) {
            var dt = Math.min(0.05, (now - last) / 1000);
            last = now;
            if (tween) {
                var k = Math.min(1, (now - tween.s) / tween.d), e = easeInOut(k);
                camera.position.lerpVectors(tween.p0, tween.p1, e);
                controls.target.lerpVectors(tween.t0, tween.t1, e);
                if (k >= 1) tween = null;
            }
            controls.update();

            var a = reduceMotion ? 1 : damp(7, dt);
            pulse += dt;
            buildings.forEach(function (b) {
                b.op += (b.opTarget - b.op) * a;
                b.floors.forEach(function (fl) {
                    var u = fl.group.userData;
                    u.y += (u.yTarget - u.y) * a;
                    u.op += (u.opTarget - u.op) * a;
                    fl.group.position.y = u.y;
                    var o = u.op * b.op;
                    fl.group.visible = o > 0.02;
                    fl.mats.forEach(function (m) { m.opacity = o * (m.userData.base || 1); });
                    fl.rooms.forEach(function (r) {
                        var m = r.mesh.material;
                        m.color.lerp(r.target, a);
                        m.opacity = o;
                        r.mesh.scale.y += ((r.hover ? 1.1 : 1) - r.mesh.scale.y) * a;
                        if (state.room === r) m.emissive.setHex(COLORS.select).multiplyScalar(0.25 + 0.15 * Math.sin(pulse * 4));
                        else m.emissive.setHex(0x000000);
                    });
                });
                b.roofY += (b.roofTargetY - b.roofY) * a;
                b.roofOp += (b.roofTargetOp * b.op - b.roofOp) * a;
                b.roof.position.y = b.floorCount * FH + 0.2 + b.roofY;
                b.roofMat.opacity = b.roofOp;
                b.roof.visible = b.roofOp > 0.02;
            });

            renderer.render(scene, camera);
            placeLabels();
            requestAnimationFrame(loop);
        }

        function start() {
            // room number tags move with their floor while it lifts away
            buildings.forEach(function (b) {
                b.floors.forEach(function (fl) {
                    fl.rooms.forEach(function (r) { if (r.tag) r.tag._floor = fl; });
                });
            });
            resize();
            var v = siteView();
            camera.position.copy(v[0]).multiplyScalar(1.3);
            camera.position.y += 20;
            controls.target.copy(v[1]);
            renderPanel();
            flyTo(v[0], v[1], 1600);
            requestAnimationFrame(loop);
            if (opts.initialRoom) {
                var r = byNumber(opts.initialRoom);
                if (r) setTimeout(function () { openRoom(r); }, reduceMotion ? 0 : 900);
            }
        }

        // ---------- room 3D (furnished model built from the room type) ----------
        var r3 = null, r3HideTimer = 0;
        function openRoom3D(r) {
            clearTimeout(r3HideTimer);
            if (r3) closeRoom3D(true);
            roomView.innerHTML = '';
            var head = el('div', 'd3-r3-head',
                '<div><div class="fw-semibold">ห้อง ' + esc(r.number) + ' · ' + esc(r.type || '') + '</div>' +
                '<div class="small text-muted">ภาพจำลองการจัดห้องตามประเภทห้อง ลากเพื่อหมุนดู</div></div>');
            var x = el('button', 'btn btn-light btn-icon', '&times;');
            x.type = 'button';
            x.setAttribute('aria-label', 'ปิดห้อง 3D');
            x.addEventListener('click', closeRoom3D);
            head.appendChild(x);
            var canvasHost = el('div', 'd3-r3-canvas');
            roomView.appendChild(head);
            roomView.appendChild(canvasHost);
            roomView.hidden = false;
            requestAnimationFrame(function () { roomView.classList.add('on'); });

            var rr = new THREE.WebGLRenderer({ antialias: true, alpha: true });
            rr.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
            rr.shadowMap.enabled = true;
            rr.shadowMap.type = THREE.PCFSoftShadowMap;
            canvasHost.appendChild(rr.domElement);
            var sc = new THREE.Scene();
            var cam = new THREE.PerspectiveCamera(40, 1, 0.1, 100);
            var ctl = new THREE.OrbitControls(cam, rr.domElement);
            ctl.enableDamping = true;
            ctl.dampingFactor = 0.08;
            ctl.enablePan = false;
            ctl.minDistance = 4;
            ctl.maxDistance = 16;
            ctl.maxPolarAngle = 1.25;
            ctl.minAzimuthAngle = -0.35;
            ctl.maxAzimuthAngle = 1.9;

            var model = buildRoomModel(r);
            sc.add(model.group);
            sc.add(new THREE.HemisphereLight(0xffffff, 0xd9cbb5, 0.75));
            var key = new THREE.DirectionalLight(0xfff4e6, 0.7);
            key.position.set(4, 8, 6);
            key.castShadow = true;
            key.shadow.mapSize.set(1024, 1024);
            key.shadow.camera.left = -6; key.shadow.camera.right = 6; key.shadow.camera.top = 6; key.shadow.camera.bottom = -6;
            sc.add(key);
            var lamp = new THREE.PointLight(0xffe2b0, 0.35, 10);
            lamp.position.set(0, 2.4, 0);
            sc.add(lamp);

            var target = new THREE.Vector3(0, 0.9, 0);
            var tall = canvasHost.clientWidth < canvasHost.clientHeight ? 1.7 : 1;
            var endPos = new THREE.Vector3(model.W * 0.9, 5.2, model.D * 1.05).multiplyScalar(tall);
            cam.position.copy(endPos).multiplyScalar(1.8);
            ctl.target.copy(target);
            var t0 = performance.now(), startPos = cam.position.clone();

            function size() {
                var w = canvasHost.clientWidth, h = canvasHost.clientHeight;
                if (!w || !h) return;
                rr.setSize(w, h, false);
                rr.domElement.style.width = w + 'px';
                rr.domElement.style.height = h + 'px';
                cam.aspect = w / h;
                cam.updateProjectionMatrix();
            }
            size();
            var ro = window.ResizeObserver ? new ResizeObserver(size) : null;
            if (ro) ro.observe(canvasHost);

            var lastT = performance.now();
            r3 = { renderer: rr, scene: sc, ro: ro, raf: 0 };
            (function frame(now) {
                var dt = Math.min(0.05, (now - lastT) / 1000);
                lastT = now;
                var k = reduceMotion ? 1 : Math.min(1, (now - t0) / 1300);
                if (k < 1) cam.position.lerpVectors(startPos, endPos, easeInOut(k));
                if (model.fan) model.fan.rotation.y += dt * 5;
                ctl.update();
                rr.render(sc, cam);
                r3.raf = requestAnimationFrame(frame);
            })(performance.now());
            x.focus();
        }
        function closeRoom3D(instant) {
            roomView.classList.remove('on');
            if (r3) {
                cancelAnimationFrame(r3.raf);
                if (r3.ro) r3.ro.disconnect();
                r3.scene.traverse(function (o) {
                    if (o.geometry) o.geometry.dispose();
                    if (o.material) {
                        (Array.isArray(o.material) ? o.material : [o.material]).forEach(function (m) { if (m.map) m.map.dispose(); m.dispose(); });
                    }
                });
                r3.renderer.dispose();
                r3 = null;
            }
            if (instant === true) return;
            r3HideTimer = setTimeout(function () { roomView.hidden = true; roomView.innerHTML = ''; }, reduceMotion ? 0 : 250);
        }

        function plankTexture() {
            var c = document.createElement('canvas');
            c.width = 256; c.height = 256;
            var g = c.getContext('2d');
            var tones = ['#d9b98f', '#d2b083', '#dfc29b', '#cfa978'];
            for (var row = 0; row < 8; row++) {
                var off = (row % 2) * 64;
                for (var col = -1; col < 3; col++) {
                    g.fillStyle = tones[(row * 3 + col + 4) % tones.length];
                    g.fillRect(col * 128 + off, row * 32, 128, 32);
                    g.strokeStyle = 'rgba(120,90,60,.25)';
                    g.strokeRect(col * 128 + off + .5, row * 32 + .5, 128, 32);
                }
            }
            var t = new THREE.CanvasTexture(c);
            t.wrapS = t.wrapT = THREE.RepeatWrapping;
            return t;
        }

        function buildRoomModel(r) {
            var big = /ใหญ่/.test(r.type || '') || r.capacity >= 3;
            var hasAir = /แอร์/.test(r.type || '');
            var W = big ? 5.4 : 4.2, D = big ? 6.6 : 5.6, H = 2.7, T = 0.12;
            var g = new THREE.Group();
            function box(w, h, d, color, x, y, z, extra) {
                var m = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), std(color, extra || {}));
                m.position.set(x, y, z);
                m.castShadow = true;
                m.receiveShadow = true;
                g.add(m);
                return m;
            }

            // floor + walls (back and left are full height; front and right are cut low like a doll house)
            var tex = plankTexture();
            tex.repeat.set(W / 2, D / 2);
            var floor = new THREE.Mesh(new THREE.BoxGeometry(W, 0.1, D), std(0xffffff, { map: tex, roughness: 0.75 }));
            floor.position.y = -0.05;
            floor.receiveShadow = true;
            g.add(floor);
            var wall = 0xf4f1ea, low = 0.35;
            box(W + T * 2, H, T, wall, 0, H / 2, -D / 2 - T / 2);
            box(T, H, D, wall, -W / 2 - T / 2, H / 2, 0);
            box(T, low, D, wall, W / 2 + T / 2, low / 2, 0);
            box(W + T * 2, low, T, wall, 0, low / 2, D / 2 + T / 2);

            // window with curtains on the back wall
            box(1.8, 1.3, 0.04, 0xbfdcf2, W * 0.15, 1.55, -D / 2 + 0.01, { roughness: 0.1, transparent: true, opacity: 0.75 });
            box(1.9, 0.06, 0.08, 0xffffff, W * 0.15, 2.23, -D / 2 + 0.04);
            box(0.35, 1.6, 0.06, 0x9fb7d8, W * 0.15 - 1.1, 1.45, -D / 2 + 0.08);
            box(0.35, 1.6, 0.06, 0x9fb7d8, W * 0.15 + 1.1, 1.45, -D / 2 + 0.08);

            // bathroom in the front-left corner
            var bw = 1.6, bd = 1.8;
            var bx = -W / 2 + bw / 2, bz = D / 2 - bd / 2;
            var tile = new THREE.Mesh(new THREE.BoxGeometry(bw, 0.02, bd), std(0xe3eef5));
            tile.position.set(bx, 0.01, bz);
            g.add(tile);
            box(bw, H, 0.08, wall, bx, H / 2, D / 2 - bd);
            box(0.08, H, bd - 0.8, wall, -W / 2 + bw, H / 2, D / 2 - (bd - 0.8) / 2);
            box(0.42, 0.42, 0.6, 0xffffff, bx - 0.35, 0.21, bz + 0.45);
            box(0.42, 0.35, 0.2, 0xffffff, bx - 0.35, 0.6, bz + 0.72);
            box(0.03, 1.9, 0.9, 0xcfe6f5, bx + 0.25, 0.95, bz - 0.3, { transparent: true, opacity: 0.45, roughness: 0.05 });
            box(0.5, 0.15, 0.38, 0xffffff, bx + 0.45, 0.85, bz + 0.65);

            // beds: one per tenant (big rooms: bunk bed + single)
            var beds = Math.max(1, Math.min(3, r.capacity || 1));
            var blanket = [0x2f80ed, 0xf5b301, 0x22a565];
            function bed(x, z, y, color) {
                box(1.0, 0.3, 2.0, 0x8b6b4a, x, y + 0.15, z);
                box(0.94, 0.2, 1.92, 0xffffff, x, y + 0.4, z);
                box(0.96, 0.06, 1.3, color, x, y + 0.53, z + 0.3);
                box(0.6, 0.12, 0.32, 0xf7f7f7, x, y + 0.56, z - 0.75);
            }
            var bedX = W / 2 - 0.6;
            bed(bedX, -D / 2 + 1.1, 0, blanket[0]);
            if (beds >= 2) {
                if (big) {
                    bed(bedX, -D / 2 + 1.1, 1.3, blanket[1]);
                    [-1, 1].forEach(function (sx) { [-1, 1].forEach(function (sz) { box(0.06, 2.0, 0.06, 0x8b6b4a, bedX + sx * 0.48, 1.0, -D / 2 + 1.1 + sz * 0.98); }); });
                } else {
                    bed(bedX - 1.25, -D / 2 + 1.1, 0, blanket[1]);
                }
            }
            if (beds >= 3) bed(bedX, -D / 2 + 3.4, 0, blanket[2]);

            // wardrobe, desk and chairs
            box(0.6, 2.0, 1.2, 0xe9e2d6, -W / 2 + 0.32, 1.0, -0.2);
            box(0.02, 1.9, 0.02, 0xb8ad9c, -W / 2 + 0.63, 1.0, -0.2);
            var desks = big ? 2 : 1;
            for (var i = 0; i < desks; i++) {
                var dz = -D / 2 + 0.35, dx = -W / 2 + 1.2 + i * 1.3;
                box(1.1, 0.05, 0.6, 0xd9c4a5, dx, 0.75, dz + 0.05);
                box(0.05, 0.72, 0.55, 0xbfa985, dx - 0.5, 0.37, dz + 0.05);
                box(0.05, 0.72, 0.55, 0xbfa985, dx + 0.5, 0.37, dz + 0.05);
                box(0.45, 0.06, 0.45, 0x5b6b80, dx, 0.46, dz + 0.65);
                box(0.45, 0.5, 0.05, 0x5b6b80, dx, 0.74, dz + 0.88);
                box(0.3, 0.2, 0.02, 0x30363f, dx, 0.92, dz - 0.1);
            }

            // rug and plant
            var rug = new THREE.Mesh(new THREE.CylinderGeometry(0.8, 0.8, 0.015, 32), std(0xc9d8ee));
            rug.position.set(0.2, 0.01, 0.6);
            rug.receiveShadow = true;
            g.add(rug);
            box(0.28, 0.32, 0.28, 0xffffff, W / 2 - 0.3, 0.16, D / 2 - 0.4);
            var leaf = new THREE.Mesh(new THREE.IcosahedronGeometry(0.3, 0), std(0x6aa36a, { flatShading: true }));
            leaf.position.set(W / 2 - 0.3, 0.6, D / 2 - 0.4);
            leaf.castShadow = true;
            g.add(leaf);

            // air conditioner or ceiling fan, depending on the room type
            var fan = null;
            if (hasAir) {
                box(1.0, 0.3, 0.24, 0xffffff, -W / 2 + 0.75, 2.3, -D / 2 + 0.14);
                box(0.9, 0.03, 0.02, 0xd5dbe3, -W / 2 + 0.75, 2.18, -D / 2 + 0.27);
            } else {
                fan = new THREE.Group();
                var hub = new THREE.Mesh(new THREE.CylinderGeometry(0.1, 0.1, 0.12, 16), std(0xffffff));
                fan.add(hub);
                for (var b = 0; b < 3; b++) {
                    var blade = new THREE.Mesh(new THREE.BoxGeometry(0.75, 0.02, 0.14), std(0xf3f3f3));
                    blade.position.x = 0.42;
                    var arm = new THREE.Group();
                    arm.rotation.y = b * Math.PI * 2 / 3;
                    arm.add(blade);
                    fan.add(arm);
                }
                fan.position.set(0.2, H - 0.25, 0.4);
                g.add(fan);
                box(0.02, 0.25, 0.02, 0xdddddd, 0.2, H - 0.12, 0.4);
            }
            return { group: g, W: W, D: D, fan: fan };
        }
    }

    window.Dorm3D = { mount: mount };
})();
