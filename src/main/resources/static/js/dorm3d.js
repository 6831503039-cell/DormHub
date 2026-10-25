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
    // colours of SJD Mansion (from the photos): cream centre building, white wings, green roof trim
    var SJD = {
        green: 0x2fa84f, cream: 0xf1e4c3, white: 0xf7f7f4, terracotta: 0xc8643c, steel: 0xc3cad2,
        glass: 0x6f8aa0, concrete: 0xd6d6d1, tank: 0x2f6fb5, column: 0xf4f2ed
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
            Object.keys(map).sort().forEach(function (id) {
                var floorsMap = map[id];
                var maxRooms = 0, maxFloor = 0, roomFloors = [];
                Object.keys(floorsMap).forEach(function (f) {
                    maxRooms = Math.max(maxRooms, floorsMap[f].length);
                    maxFloor = Math.max(maxFloor, +f);
                    roomFloors.push(+f);
                });
                roomFloors.sort(function (a, b) { return a - b; });
                var cols = Math.max(1, Math.ceil(maxRooms / 2));
                buildings.push({ id: id, floorsMap: floorsMap, cols: cols, L: cols * ROOM_W, floorCount: maxFloor, roomFloors: roomFloors });
            });

            // U-shaped site like SJD: A (cream, with the sign) at the back, wings C (left) and B (right) facing the courtyard
            var A = find('A');
            var topZ = -14;
            var aLen = A ? A.L : 14;
            var sideX = aLen / 2 + DEPTH / 2 + 1.2;
            var wingEnd = topZ + DEPTH / 2 + 6, extra = 0;
            buildings.forEach(function (b) {
                if (b.id === 'A') { b.axis = 'x'; b.front = 1; b.cx = 0; b.cz = topZ; }
                else if (b.id === 'B' || b.id === 'C') {
                    b.axis = 'z'; b.front = b.id === 'B' ? -1 : 1; b.cx = b.id === 'B' ? sideX : -sideX;
                    b.cz = topZ - DEPTH / 2 + b.L / 2;
                    wingEnd = Math.max(wingEnd, topZ - DEPTH / 2 + b.L);
                }
                else { b.axis = 'x'; b.front = -1; b.cx = (extra - 0.5) * (b.L + 6); b.cz = wingEnd + 22; extra++; }
            });

            buildSite(sideX - DEPTH / 2, topZ + DEPTH / 2, wingEnd);
            buildings.forEach(buildBuilding);
            refreshColors(true);
        }
        function find(id) { for (var i = 0; i < buildings.length; i++) if (buildings[i].id === id) return buildings[i]; return null; }
        function wallColor(b) { return b.id === 'A' ? SJD.cream : SJD.white; }

        function car(x, z, rot, color) {
            var g = new THREE.Group();
            var body = new THREE.Mesh(new THREE.BoxGeometry(1.8, 0.7, 4.3), std(color, { roughness: 0.4, metalness: 0.3 }));
            body.position.y = 0.6;
            var cabin = new THREE.Mesh(new THREE.BoxGeometry(1.6, 0.55, 2.3), std(0x3b4652, { roughness: 0.2 }));
            cabin.position.set(0, 1.2, -0.2);
            body.castShadow = cabin.castShadow = true;
            g.add(body); g.add(cabin);
            [-0.8, 0.8].forEach(function (sx) {
                [-1.4, 1.4].forEach(function (sz) {
                    var w = new THREE.Mesh(new THREE.CylinderGeometry(0.33, 0.33, 0.2, 12), std(0x1f2225));
                    w.rotation.z = Math.PI / 2;
                    w.position.set(sx, 0.33, sz);
                    g.add(w);
                });
            });
            g.position.set(x, 0, z);
            g.rotation.y = rot;
            return g;
        }

        function buildSite(halfW, z0, z1) {
            var ground = new THREE.Mesh(new THREE.PlaneGeometry(320, 320), std(0xe3e8e1, { roughness: 1 }));
            ground.rotation.x = -Math.PI / 2;
            ground.receiveShadow = true;
            scene.add(ground);

            // concrete courtyard / parking, open towards the road (+z)
            var len = z1 - z0 + 14;
            var yard = new THREE.Mesh(new THREE.PlaneGeometry(halfW * 2 + DEPTH * 2 + 6, len), std(SJD.concrete, { roughness: 1 }));
            yard.rotation.x = -Math.PI / 2;
            yard.position.set(0, 0.01, z0 - DEPTH + len / 2);
            yard.receiveShadow = true;
            scene.add(yard);

            // parking lines along both wings
            var lineM = std(0xffffff, { roughness: 1 });
            for (var z = z0 + 6; z < z1 - 1; z += 2.7) {
                [-1, 1].forEach(function (s) {
                    var ln = new THREE.Mesh(new THREE.PlaneGeometry(4.6, 0.1), lineM);
                    ln.rotation.x = -Math.PI / 2;
                    ln.position.set(s * (halfW - 2.6), 0.02, z);
                    scene.add(ln);
                });
            }
            var cars = [[-1, 0.35, 0xc3c7cc], [-1, 0.62, 0x26292d], [1, 0.48, 0xc3c7cc], [1, 0.8, 0xf0f0ee], [-1, 0.9, 0x9aa3ad]];
            cars.forEach(function (c) {
                scene.add(car(c[0] * (halfW - 2.6), z0 + 4 + c[1] * (z1 - z0 - 4), c[0] * Math.PI / 2, c[2]));
            });

            // hedge planter in front of building A, like the photos
            var planter = new THREE.Mesh(new THREE.BoxGeometry(Math.min(8, halfW), 0.55, 2.4), std(SJD.terracotta));
            planter.position.set(0, 0.28, z0 + 3.2);
            planter.castShadow = planter.receiveShadow = true;
            scene.add(planter);
            var hedge = new THREE.Mesh(new THREE.SphereGeometry(1, 20, 12), std(0x4c8a3a, { roughness: 1 }));
            hedge.scale.set(Math.min(8, halfW) / 2 - 0.1, 0.75, 1.1);
            hedge.position.set(0, 0.85, z0 + 3.2);
            hedge.castShadow = true;
            scene.add(hedge);

            // umbrella tables in the corners of the courtyard
            [[-1, 1.2], [1, 1.2], [-1, 5.5]].forEach(function (p, k) {
                var x = p[0] * (halfW - 1.6), zz = z0 + p[1];
                var top = new THREE.Mesh(new THREE.CylinderGeometry(0.55, 0.55, 0.06, 16), std(0xf5f5f2));
                top.position.set(x, 0.75, zz);
                var pole = new THREE.Mesh(new THREE.CylinderGeometry(0.04, 0.04, 2.2, 8), std(0xdddddd));
                pole.position.set(x, 1.1, zz);
                var umb = new THREE.Mesh(new THREE.ConeGeometry(1.2, 0.5, 8), std(k % 2 ? SJD.green : 0xf5f5f2));
                umb.position.set(x, 2.3, zz);
                umb.castShadow = true;
                scene.add(top); scene.add(pole); scene.add(umb);
            });

            var tag = el('span', 'd3-lbl d3-lbl-site', 'ลานจอดรถ / ส่วนกลาง');
            addLabel(tag, new THREE.Vector3(0, 0.5, (z0 + z1) / 2 + 2), function () { return state.view === 'site'; }, 'center');
        }

        function signTexture(top, bottom) {
            var c = document.createElement('canvas');
            c.width = 512; c.height = 320;
            var g = c.getContext('2d');
            g.fillStyle = '#2fa84f';
            g.fillRect(0, 0, 512, 320);
            g.fillStyle = '#ffffff';
            g.fillRect(28, 28, 456, 264);
            g.fillStyle = '#2fa84f';
            g.fillRect(40, 40, 432, 240);
            g.fillStyle = '#ffffff';
            g.textAlign = 'center';
            g.textBaseline = 'middle';
            g.font = 'bold 150px "IBM Plex Sans Thai", sans-serif';
            g.fillText(top, 256, 135);
            g.font = '600 64px "IBM Plex Sans Thai", sans-serif';
            g.fillText(bottom, 256, 240);
            return new THREE.CanvasTexture(c);
        }

        function buildBuilding(b) {
            b.group = new THREE.Group();
            b.op = 1; b.opTarget = 1;
            b.floors = [];
            var rd = (DEPTH - CORRIDOR) / 2;
            var c = toWorld(b, 0, 0);
            var wall = wallColor(b);
            b.center = new THREE.Vector3(c[0], 0, c[1]);

            // geometries shared by every floor of this building
            function geo(lu, h, lv) { var d = dims(b, lu, lv); return new THREE.BoxGeometry(d[0], h, d[1]); }
            var G = {
                slab: geo(b.L + 0.6, 0.35, DEPTH + 0.6),
                end: geo(0.25, FH - 0.35, DEPTH),
                balcony: geo(ROOM_W - 0.1, 0.14, 1.15),
                bar: geo(ROOM_W - 0.25, 0.045, 0.045),
                fin: geo(0.14, FH - 0.35, 1.2),
                door: geo(ROOM_W * 0.55, 2.1, 0.05),
                win: geo(1.4, 1.1, 0.05),
                ac: geo(0.75, 0.5, 0.3),
                column: new THREE.BoxGeometry(0.45, FH - 0.35, 0.45),
                base: new THREE.BoxGeometry(0.58, 0.75, 0.58),
                back: geo(b.L, FH - 0.35, 0.25)
            };
            function put(g, geom, mat, u, y, v, shadow) {
                var p = toWorld(b, u, v);
                var m = new THREE.Mesh(geom, mat);
                m.position.set(p[0], y, p[1]);
                if (shadow !== false) { m.castShadow = true; m.receiveShadow = true; }
                g.add(m);
                return m;
            }

            for (var f = 1; f <= b.floorCount; f++) {
                var g = new THREE.Group();
                var y0 = (f - 1) * FH;
                var M = {
                    wall: std(wall, { transparent: true }),
                    steel: std(SJD.steel, { transparent: true, metalness: 0.6, roughness: 0.3 }),
                    glass: std(SJD.glass, { transparent: true, roughness: 0.15 }),
                    ac: std(0xe4e7ea, { transparent: true }),
                    column: std(SJD.column, { transparent: true }),
                    terracotta: std(SJD.terracotta, { transparent: true })
                };
                var mats = Object.keys(M).map(function (k) { return M[k]; });
                put(g, G.slab, M.wall, 0, y0 + 0.175, 0);

                var list = b.floorsMap[f] || [];
                if (!list.length) {
                    // ground floor without rooms: open parking under the building, white columns on terracotta bases
                    for (var i = 0; i <= b.cols; i++) {
                        var u = -b.L / 2 + i * ROOM_W;
                        [DEPTH / 2 - 0.4, 0.2].forEach(function (v) {
                            put(g, G.column, M.column, u, y0 + 0.35 + (FH - 0.35) / 2, v);
                            put(g, G.base, M.terracotta, u, y0 + 0.35 + 0.375, v);
                        });
                    }
                    put(g, G.back, M.terracotta, 0, y0 + 0.35 + (FH - 0.35) / 2, -DEPTH / 2 + 0.3);
                    if (b.cols >= 3) {
                        var p1 = toWorld(b, -b.L / 4, DEPTH / 4 - 0.6), p2 = toWorld(b, b.L / 5, DEPTH / 4 - 0.6);
                        var rot = b.axis === 'x' ? 0 : Math.PI / 2;
                        g.add(car(p1[0], p1[1], rot + Math.PI / 2, 0xf0f0ee));
                        g.add(car(p2[0], p2[1], rot + Math.PI / 2, 0x2b2f34));
                    }
                } else {
                    put(g, G.end, M.wall, -b.L / 2 + 0.12, y0 + 0.35 + (FH - 0.35) / 2, 0);
                    put(g, G.end, M.wall, b.L / 2 - 0.12, y0 + 0.35 + (FH - 0.35) / 2, 0);
                }

                list.forEach(function (r, j) {
                    var side = j < b.cols ? 0 : 1;
                    var i = j % b.cols;
                    var u = -b.L / 2 + ROOM_W * (i + 0.5);
                    var v = (side === 0 ? 1 : -1) * (CORRIDOR / 2 + rd / 2);
                    var p = toWorld(b, u, v);
                    var d = dims(b, ROOM_W - 0.24, rd - 0.24);
                    var rgeo = new THREE.BoxGeometry(d[0], 2.55, d[1]);
                    rgeo.translate(0, 1.275, 0);
                    var mesh = new THREE.Mesh(rgeo, std(wall, { transparent: true, roughness: 0.7 }));
                    mesh.position.set(p[0], y0 + 0.35, p[1]);
                    mesh.castShadow = mesh.receiveShadow = true;
                    mesh.userData.room = r;
                    r.mesh = mesh;
                    r.side = side;
                    r.target = new THREE.Color(wall);
                    r.hover = 0;
                    r.pos = new THREE.Vector3(p[0], y0 + 3.1, p[1]);
                    r.b = b;
                    g.add(mesh);
                    roomMeshes.push(mesh);
                    allRooms.push(r);

                    if (side === 0) {
                        // courtyard side: balcony with stainless railing, sliding door and AC unit
                        var bv = DEPTH / 2 + 0.55;
                        put(g, G.balcony, M.wall, u, y0 + 0.28, bv);
                        [0.5, 0.8, 1.1].forEach(function (h) { put(g, G.bar, M.steel, u, y0 + 0.35 + h, DEPTH / 2 + 1.08, false); });
                        put(g, G.fin, M.wall, u - ROOM_W / 2 + 0.07, y0 + 0.35 + (FH - 0.35) / 2, DEPTH / 2 + 0.5);
                        if (i === b.cols - 1) put(g, G.fin, M.wall, u + ROOM_W / 2 - 0.07, y0 + 0.35 + (FH - 0.35) / 2, DEPTH / 2 + 0.5);
                        put(g, G.door, M.glass, u - 0.4, y0 + 0.35 + 1.05, DEPTH / 2 - 0.08, false);
                        put(g, G.ac, M.ac, u + ROOM_W / 2 - 0.65, y0 + 0.35 + 0.25, DEPTH / 2 + 0.4);
                    } else {
                        put(g, G.win, M.glass, u, y0 + 0.35 + 1.5, -DEPTH / 2 + 0.08, false);
                        put(g, G.ac, M.ac, u + 1.1, y0 + 0.35 + 0.6, -DEPTH / 2 - 0.12);
                    }

                    if (r.status !== 'MAINTENANCE' || isAdmin) {
                        var tag = el('span', 'd3-lbl d3-lbl-room', esc(r.number));
                        r.tag = tag;
                        addLabel(tag, r.pos, (function (ff) {
                            return function () { return state.view === 'floor' && state.b === b.id && state.f === ff; };
                        })(f), 'center');
                    }
                });

                g.userData = { n: f, y: 0, yTarget: 0, op: 1, opTarget: 1 };
                b.floors.push({ n: f, group: g, mats: mats, rooms: list });
                b.group.add(g);

                if (list.length) {
                    // clickable floor chip
                    var chip = el('button', 'd3-lbl d3-lbl-floor', 'ชั้น ' + f);
                    chip.type = 'button';
                    chip.addEventListener('click', (function (ff) { return function () { goFloor(ff); }; })(f));
                    var endPos = b.axis === 'x'
                        ? new THREE.Vector3(b.cx + b.L / 2 + 1.1, y0 + 1.7, b.cz + (DEPTH / 2 + 1) * b.front)
                        : new THREE.Vector3(b.cx + (DEPTH / 2 + 1) * b.front, y0 + 1.7, b.cz + b.L / 2 + 1.1);
                    (function (ff, chipEl) {
                        var lb = addLabel(chipEl, endPos, function () {
                            return state.view !== 'site' && state.b === b.id && (state.f == null || ff <= state.f);
                        }, 'left');
                        lb.after = function () { chipEl.classList.toggle('active', state.f === ff); };
                    })(f, chip);
                }
            }

            // roof: green parapet, stair/water towers with blue tanks, and the dorm sign on building A
            var roof = new THREE.Group();
            var RM = {
                wall: std(wall, { transparent: true }),
                green: std(SJD.green, { transparent: true }),
                tank: std(SJD.tank, { transparent: true, roughness: 0.4 }),
                white: std(SJD.white, { transparent: true })
            };
            b.roofMats = Object.keys(RM).map(function (k) { return RM[k]; });
            put(roof, G.slab, RM.wall, 0, 0.2, 0);
            var pg1 = geo(b.L + 0.7, 0.5, 0.18), pg2 = geo(0.18, 0.5, DEPTH + 0.7);
            put(roof, pg1, RM.green, 0, 0.6, DEPTH / 2 + 0.25);
            put(roof, pg1, RM.green, 0, 0.6, -DEPTH / 2 - 0.25);
            put(roof, pg2, RM.green, -b.L / 2 - 0.25, 0.6, 0);
            put(roof, pg2, RM.green, b.L / 2 + 0.25, 0.6, 0);
            function tanks(u, v, y) {
                [-0.7, 0.7].forEach(function (o) {
                    var t = new THREE.Mesh(new THREE.CylinderGeometry(0.55, 0.55, 1.3, 16), RM.tank);
                    var p = toWorld(b, u + o, v);
                    t.position.set(p[0], y + 0.65, p[1]);
                    t.castShadow = true;
                    roof.add(t);
                });
            }
            if (b.axis === 'z') {
                // stair tower at the courtyard end of each wing
                var tu = b.L / 2 - 1.8;
                put(roof, geo(3.2, 3, 3.2), RM.white, tu, 1.5, 0);
                put(roof, geo(3.5, 0.5, 3.5), RM.green, tu, 3.1, 0);
                [-1, 1].forEach(function (su) { [-1, 1].forEach(function (sv) { put(roof, geo(0.3, 3, 0.3), RM.green, tu + su * 1.6, 1.5, sv * 1.6); }); });
                tanks(tu, 0, 3.35);
            } else {
                // sign tower in the middle of building A, facing the courtyard
                put(roof, geo(4.2, 4.6, 3), RM.green, 0, 2.3, DEPTH / 2 - 1.5);
                var sign = new THREE.Mesh(new THREE.PlaneGeometry(3.8, 2.4), new THREE.MeshBasicMaterial({ map: signTexture(opts.signTop || 'SJD', opts.signBottom || 'แมนชั่น'), transparent: true }));
                b.roofMats.push(sign.material);
                var sp = toWorld(b, 0, DEPTH / 2 + 0.02);
                sign.position.set(sp[0], 2.6, sp[1]);
                if (b.front < 0) sign.rotation.y = Math.PI;
                roof.add(sign);
                tanks(0, DEPTH / 2 - 1.5, 4.6);
            }
            b.roof = roof;
            b.roofY = 0; b.roofOp = 1;
            roof.position.y = b.floorCount * FH;
            b.group.add(roof);
            b.group.traverse(function (o) { o.userData.building = b.id; });
            scene.add(b.group);

            var tag = el('button', 'd3-lbl d3-lbl-building');
            tag.type = 'button';
            tag.addEventListener('click', function () { goBuilding(b.id); });
            b.tag = tag;
            addLabel(tag, b.center.clone().setY(b.floorCount * FH + (b.axis === 'x' ? 7 : 5)), function () { return state.view === 'site'; }, 'top');
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
                b.opTarget = state.b && !active ? 0 : 1; // other buildings fade out so the chosen one is clear
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
                // overview shows the real building; the selected building shows each room's status colour
                var active = state.b === r.b.id;
                var c = wallColor(r.b);
                if (active) c = state.onlyBookable && !bookable(r) ? COLORS.neutral : statusColor(r.status);
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
                ? [new THREE.Vector3(-10, 92, 92), new THREE.Vector3(0, 0, 4)]
                : [new THREE.Vector3(-12, 46, 50), new THREE.Vector3(-6, 4, -3)];
        }
        function buildingView(b, floor) {
            var out = b.axis === 'x' ? new THREE.Vector3(0, 0, b.front) : new THREE.Vector3(b.front, 0, 0);
            var side = b.axis === 'x' ? new THREE.Vector3(1, 0, 0) : new THREE.Vector3(0, 0, 1);
            var far = narrow() ? 2 : 1;
            var t, p;
            if (floor == null) {
                t = b.center.clone().setY(b.floorCount * FH / 2);
                p = t.clone().addScaledVector(out, (18 + b.L) * far).addScaledVector(side, -7).setY(t.y + 16 * far);
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
                for (var k = b.roomFloors.length - 1; k >= 0; k--) {
                    var f = b.roomFloors[k];
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
                b.roof.position.y = b.floorCount * FH + b.roofY;
                b.roofMats.forEach(function (m) { m.opacity = b.roofOp; });
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
                '<div class="small text-muted">ภาพจำลองจากห้องจริงของหอ (เฟอร์นิเจอร์ตามประเภทห้อง) ลากเพื่อหมุนดู</div></div>');
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
            var tall = canvasHost.clientWidth < canvasHost.clientHeight ? 2.1 : 1;
            var endPos = new THREE.Vector3(model.W * 0.8, 4.6, model.D * 0.78).multiplyScalar(tall);
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

        function plankTexture(tones, seam) {
            var c = document.createElement('canvas');
            c.width = 256; c.height = 256;
            var g = c.getContext('2d');
            for (var row = 0; row < 8; row++) {
                var off = (row % 2) * 64;
                for (var col = -1; col < 3; col++) {
                    g.fillStyle = tones[(row * 3 + col + 4) % tones.length];
                    g.fillRect(col * 128 + off, row * 32, 128, 32);
                    g.strokeStyle = seam;
                    g.strokeRect(col * 128 + off + .5, row * 32 + .5, 128, 32);
                }
            }
            var t = new THREE.CanvasTexture(c);
            t.wrapS = t.wrapT = THREE.RepeatWrapping;
            return t;
        }

        /*
         * Furnished room modelled on the SJD room photos: pale wood-look tiles, white walls with an orange or green
         * accent wall behind the beds, wooden beds with white-panel headboards and black leather mattresses,
         * mirror wardrobe, long desk with two chairs and a TV, black mini fridge with an orange door, split AC
         * above the sliding balcony door with brown curtains, balcony with stainless railing and sink, bathroom.
         * Back wall (z < 0) faces the balcony, front wall (z > 0) has the entrance; front and right walls are cut low.
         */
        function buildRoomModel(r) {
            var big = /ใหญ่/.test(r.type || '') || r.capacity >= 3;
            var hasAir = !/พัดลม/.test(r.type || '');
            var W = big ? 5.0 : 4.2, D = big ? 6.8 : 6.2, H = 2.7, T = 0.12;
            var hash = 0;
            for (var i = 0; i < r.number.length; i++) hash = (hash * 31 + r.number.charCodeAt(i)) % 997;
            var accent = hash % 3 === 0 ? 0x62c23c : 0xe8762c;
            var g = new THREE.Group();
            function box(w, h, d, color, x, y, z, extra) {
                var m = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), std(color, extra || {}));
                m.position.set(x, y, z);
                m.castShadow = true;
                m.receiveShadow = true;
                g.add(m);
                return m;
            }
            var WHITE = 0xf5f4f0, WOOD = 0x8d6e55, WOOD_LIGHT = 0xa88a6c, PANEL = 0xf1efe9, MATTRESS = 0x2a2422;
            var x0 = -W / 2, x1 = W / 2, zB = -D / 2, zF = D / 2;

            // floor tiles and walls
            var tex = plankTexture(['#e2d2b6', '#dbcaad', '#e8d9c0', '#decdb2'], 'rgba(140,115,85,.35)');
            tex.repeat.set(W / 2, D / 2);
            var floor = new THREE.Mesh(new THREE.BoxGeometry(W, 0.1, D), std(0xffffff, { map: tex, roughness: 0.55 }));
            floor.position.y = -0.05;
            floor.receiveShadow = true;
            g.add(floor);
            var low = 0.35;
            box(T, H, D, accent, x0 - T / 2, H / 2, 0);                         // accent wall behind the beds
            box(T, low, D, WHITE, x1 + T / 2, low / 2, 0);                      // right wall (cut)
            box(W + T * 2, low, T, WHITE, 0, low / 2, zF + T / 2);              // front wall (cut)
            [[x0, 0, D, 'x'], [0, zB, W, 'z']].forEach(function (s) {           // dark skirting
                if (s[3] === 'x') box(0.02, 0.08, s[2], 0x5b3a28, s[0] + 0.01, 0.04, s[1]);
                else box(s[2], 0.08, 0.02, 0x5b3a28, s[0], 0.04, s[1] + 0.01);
            });

            // back wall with the sliding balcony door, bathroom in the back-right corner
            var bw = 1.5, bd = 1.8;
            var doorX0 = x0 + 0.7, doorX1 = x1 - bw - 0.35;
            box(doorX0 - x0, H, T, WHITE, (x0 + doorX0) / 2, H / 2, zB - T / 2);
            box(x1 - doorX1, H, T, WHITE, (doorX1 + x1) / 2, H / 2, zB - T / 2);
            box(doorX1 - doorX0, H - 2.15, T, WHITE, (doorX0 + doorX1) / 2, 2.15 + (H - 2.15) / 2, zB - T / 2);
            var dw = doorX1 - doorX0;
            [doorX0 + dw * 0.25, doorX0 + dw * 0.75].forEach(function (x) {
                box(dw / 2 - 0.04, 2.1, 0.03, 0xcfe6f2, x, 1.05, zB - T / 2, { transparent: true, opacity: 0.35, roughness: 0.05 });
            });
            [doorX0, doorX0 + dw / 2, doorX1].forEach(function (x) { box(0.06, 2.15, 0.08, 0xf4f4f4, x, 1.075, zB - T / 2); });
            box(dw, 0.06, 0.08, 0xf4f4f4, (doorX0 + doorX1) / 2, 2.15, zB - T / 2);
            box(dw + 0.5, 0.03, 0.03, 0xd0d0d0, (doorX0 + doorX1) / 2, 2.35, zB + 0.12);   // curtain rod
            for (var k = 0; k < 6; k++) {                                                 // brown curtain, pulled to the left
                box(0.13, 2.2, 0.08, k % 2 ? 0x7b6a5a : 0x857463, doorX0 - 0.25 + k * 0.12, 1.22, zB + 0.12 + (k % 2) * 0.04);
            }
            if (hasAir) {
                box(0.95, 0.3, 0.22, 0xfbfbfb, (doorX0 + doorX1) / 2 + 0.2, 2.47, zB + 0.11);
                box(0.85, 0.03, 0.02, 0xd5dbe3, (doorX0 + doorX1) / 2 + 0.2, 2.35, zB + 0.23);
            }

            // balcony: wood-look floor, stainless railing, small sink on a green counter
            var balTex = plankTexture(['#b98d62', '#b0845a', '#c0956b', '#a97d55'], 'rgba(80,50,30,.3)');
            balTex.repeat.set(W / 2, 0.6);
            var bal = new THREE.Mesh(new THREE.BoxGeometry(W, 0.1, 1.25), std(0xffffff, { map: balTex, roughness: 0.7 }));
            bal.position.set(0, -0.08, zB - T - 0.62);
            bal.receiveShadow = true;
            g.add(bal);
            box(T, H, 1.25, WHITE, x0 - T / 2, H / 2, zB - T - 0.62);
            [0.3, 0.55, 0.8, 1.05].forEach(function (h) { box(W, 0.035, 0.035, 0xc9cfd6, 0, h, zB - T - 1.2, { metalness: 0.7, roughness: 0.25 }); });
            [x0 + 0.05, 0, x1 - 0.05].forEach(function (x) { box(0.05, 1.1, 0.05, 0xc9cfd6, x, 0.55, zB - T - 1.2, { metalness: 0.7, roughness: 0.25 }); });
            box(0.5, 0.08, 0.7, 0x46c23c, x1 - 0.3, 0.85, zB - T - 0.62);
            box(0.36, 0.1, 0.45, 0xd9dde1, x1 - 0.3, 0.87, zB - T - 0.62, { metalness: 0.6, roughness: 0.3 });
            box(0.08, 0.85, 0.08, 0xdddddd, x1 - 0.3, 0.42, zB - T - 0.62);

            // bathroom (back-right corner) with dark door frame and toilet
            var bx0 = x1 - bw, bz1 = zB + bd;
            var tile = new THREE.Mesh(new THREE.BoxGeometry(bw, 0.02, bd), std(0xe9eef1));
            tile.position.set(bx0 + bw / 2, 0.01, zB + bd / 2);
            g.add(tile);
            box(0.1, H, bd, WHITE, bx0, H / 2, zB + bd / 2);
            box(bw - 0.8, H, 0.1, WHITE, bx0 + 0.8 + (bw - 0.8) / 2, H / 2, bz1);
            box(0.8, H - 2.05, 0.1, WHITE, bx0 + 0.4, 2.05 + (H - 2.05) / 2, bz1);
            [bx0 + 0.07, bx0 + 0.78].forEach(function (x) { box(0.06, 2.05, 0.14, 0x2e2e30, x, 1.025, bz1); });
            box(0.77, 0.06, 0.14, 0x2e2e30, bx0 + 0.42, 2.05, bz1);
            var door = box(0.04, 2.0, 0.68, 0xf2f2f2, bx0 + 0.12, 1.0, bz1 + 0.36);
            door.rotation.y = 0.25;
            box(0.42, 0.42, 0.55, 0xffffff, x1 - 0.35, 0.21, zB + 0.45);
            box(0.42, 0.38, 0.2, 0xffffff, x1 - 0.35, 0.62, zB + 0.12);
            box(bw - 0.12, 1.4, 0.02, 0x8a7b6e, bx0 + bw / 2, 1.3, zB + 0.02);       // tile accent strip

            // beds against the accent wall: double bed for 1 person, single beds for shared rooms
            function nightstand(z) {
                box(0.42, 0.5, 0.45, WOOD, x0 + 0.24, 0.25, z);
                box(0.02, 0.16, 0.36, 0xf3f3f0, x0 + 0.46, 0.38, z);
            }
            function bed(zc, width) {
                var len = 2.0;
                box(len + 0.08, 0.28, width + 0.08, WOOD_LIGHT, x0 + 0.06 + len / 2, 0.2, zc);
                box(len, 0.22, width, MATTRESS, x0 + 0.08 + len / 2, 0.45, zc, { roughness: 0.45 });
                box(0.12, 1.05, width + 0.12, WOOD, x0 + 0.06, 0.52, zc);
                var n = 3, pw = (width - 0.3) / n;
                for (var p = 0; p < n; p++) box(0.02, 0.38, pw - 0.06, PANEL, x0 + 0.13, 0.78, zc - (width - 0.3) / 2 + pw * (p + 0.5));
            }
            var people = Math.max(1, Math.min(3, r.capacity || 1));
            var zStart = zB + 1.0;
            if (people === 1) {
                bed(zStart + 0.9, 1.7);
                nightstand(zStart + 2.05);
            } else {
                for (var b2 = 0; b2 < people; b2++) {
                    var zc = zStart + 0.55 + b2 * 1.5;
                    bed(zc, 1.05);
                    if (b2 < people - 1) nightstand(zc + 0.75);
                }
            }

            // wardrobe with mirror door (front-left corner)
            var wx = x0 + 0.75, wz = zF - 0.33;
            box(1.3, 2.05, 0.6, WOOD, wx, 1.025, wz);
            box(0.42, 1.95, 0.02, 0x8b6b52, wx - 0.43, 1.03, wz - 0.31);
            box(0.42, 1.95, 0.02, 0xf4f3ef, wx, 1.03, wz - 0.31);
            box(0.42, 1.95, 0.02, 0xc9d3da, wx + 0.43, 1.03, wz - 0.31, { metalness: 0.7, roughness: 0.1 });
            box(0.02, 0.25, 0.02, 0x9aa0a6, wx - 0.05, 1.05, wz - 0.33);
            box(0.02, 0.25, 0.02, 0x9aa0a6, wx + 0.05, 1.05, wz - 0.33);

            // entrance door (cream panel door) on the front wall
            var ex = x1 - 1.0;
            box(0.95, 2.1, 0.06, 0xe9dfc3, ex, 1.05, zF - 0.02);
            box(1.05, 0.06, 0.1, 0x2e2e30, ex, 2.13, zF);
            [ex - 0.5, ex + 0.5].forEach(function (x) { box(0.05, 2.13, 0.1, 0x2e2e30, x, 1.065, zF); });
            box(0.36, 0.26, 0.08, 0xeeeeea, ex - 0.9, 2.3, zF - 0.02);       // electrical box

            // black mini fridge with orange door, next to the bathroom
            var fz = bz1 + 0.4;
            box(0.5, 0.85, 0.5, 0x1f1f21, x1 - 0.3, 0.425, fz);
            box(0.03, 0.72, 0.42, 0xe8702a, x1 - 0.56, 0.43, fz);

            // long desk along the right wall: white drawers, open shelf, two chairs, TV and Wi-Fi router
            var dz0 = fz + 0.4, dLen = Math.min(2.2, zF - 1.4 - dz0), dzc = dz0 + dLen / 2, dx = x1 - 0.32;
            if (dLen > 1.2) {
                box(0.62, 0.05, dLen, WOOD, dx, 0.75, dzc);
                box(0.02, 0.14, 0.5, 0xf3f3f0, dx - 0.31, 0.65, dz0 + 0.3);
                box(0.02, 0.14, 0.5, 0xf3f3f0, dx - 0.31, 0.65, dz0 + dLen - 0.3);
                box(0.55, 0.72, 0.04, WOOD, dx, 0.36, dz0 + 0.02);
                box(0.55, 0.72, 0.04, WOOD, dx, 0.36, dz0 + dLen - 0.02);
                box(0.55, 0.72, 0.5, 0x7d604a, dx + 0.02, 0.36, dzc);
                [0.25, 0.5].forEach(function (h) { box(0.5, 0.025, 0.46, 0xa58468, dx - 0.02, h, dzc); });
                box(0.06, 0.45, 0.8, 0x18191b, x1 - 0.2, 1.05, dzc, { roughness: 0.3 });
                box(0.2, 0.04, 0.3, 0x18191b, x1 - 0.2, 0.79, dzc);
                box(0.15, 0.04, 0.22, 0xfafafa, dx, 0.8, dz0 + 0.25);
                [dz0 + 0.45, dz0 + dLen - 0.45].forEach(function (cz) {
                    if (cz < dzc - 0.3 || cz > dzc + 0.3) {
                        box(0.45, 0.05, 0.42, 0xc8beb0, dx - 0.65, 0.45, cz);
                        box(0.05, 0.5, 0.42, 0xc8beb0, dx - 0.9, 0.72, cz);
                        [-0.18, 0.18].forEach(function (sx) { [-0.16, 0.16].forEach(function (sz) { box(0.04, 0.43, 0.04, 0xbdb3a5, dx - 0.65 + sx, 0.215, cz + sz); }); });
                    }
                });
            }

            // ceiling fan instead of AC in fan rooms
            var fan = null;
            if (!hasAir) {
                fan = new THREE.Group();
                fan.add(new THREE.Mesh(new THREE.CylinderGeometry(0.1, 0.1, 0.12, 16), std(0xffffff)));
                for (var bl = 0; bl < 3; bl++) {
                    var blade = new THREE.Mesh(new THREE.BoxGeometry(0.75, 0.02, 0.14), std(0xf3f3f3));
                    blade.position.x = 0.42;
                    var arm = new THREE.Group();
                    arm.rotation.y = bl * Math.PI * 2 / 3;
                    arm.add(blade);
                    fan.add(arm);
                }
                fan.position.set(0, H - 0.25, 0.3);
                g.add(fan);
            }
            g.position.z = 0.6; // keep the balcony inside the view
            return { group: g, W: W, D: D + 1.2, fan: fan };
        }
    }

    window.Dorm3D = { mount: mount };
})();
