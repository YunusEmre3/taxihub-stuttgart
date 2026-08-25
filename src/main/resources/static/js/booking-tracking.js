(function () {
    var STUTTGART_CENTER = [48.7758, 9.1829];

    var mapEl = document.getElementById("trackingMap");
    var pickupAddress = mapEl.getAttribute("data-pickup-address");
    var dropoffAddress = mapEl.getAttribute("data-dropoff-address");
    var vehicleType = mapEl.getAttribute("data-vehicle-type");
    var driverLat = parseFloat(mapEl.getAttribute("data-driver-lat"));
    var driverLng = parseFloat(mapEl.getAttribute("data-driver-lng"));

    var map = L.map("trackingMap").setView(STUTTGART_CENTER, 12);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: "&copy; OpenStreetMap contributors",
        maxZoom: 19
    }).addTo(map);

    var driverIcon = L.divIcon({ className: "", html: '<div class="dispatch-marker-driver">🚕</div>', iconSize: [30, 30], iconAnchor: [15, 15] });

    var routeStatus = document.getElementById("trackingRouteStatus");
    var distanceStat = document.getElementById("trackingDistanceStat");
    var durationStat = document.getElementById("trackingDurationStat");
    var priceStat = document.getElementById("trackingPriceStat");

    var csrfToken = document.querySelector('meta[name="_csrf"]');
    var csrfHeader = document.querySelector('meta[name="_csrf_header"]');

    var bounds = [];

    if (!isNaN(driverLat) && !isNaN(driverLng)) {
        L.marker([driverLat, driverLng], { icon: driverIcon })
            .addTo(map)
            .bindPopup("<strong>Sürücünün son bilinen konumu</strong>");
        bounds.push([driverLat, driverLng]);
    }

    function setError(message) {
        routeStatus.textContent = message;
        routeStatus.classList.add("visible");
        distanceStat.textContent = "-";
        durationStat.textContent = "-";
        priceStat.textContent = "-";
    }

    function loadRoute() {
        var headers = { "Content-Type": "application/json" };
        if (csrfToken && csrfHeader) {
            headers[csrfHeader.content] = csrfToken.content;
        }

        fetch("/api/bookings/calculate-route", {
            method: "POST",
            headers: headers,
            body: JSON.stringify({ pickupAddress: pickupAddress, dropoffAddress: dropoffAddress, vehicleType: vehicleType })
        })
            .then(function (response) { return response.json(); })
            .then(function (result) {
                if (!result.success) {
                    setError(result.errorMessage || "Rota hesaplanamadı.");
                    if (bounds.length > 0) {
                        map.fitBounds(bounds, { padding: [40, 40], maxZoom: 14 });
                    }
                    return;
                }

                var latLngs = result.path.map(function (point) { return [point.lat, point.lon]; });
                L.polyline(latLngs, { color: "#1565C0", weight: 5 }).addTo(map);
                L.marker([result.pickup.lat, result.pickup.lon]).addTo(map).bindPopup("Alış Noktası");
                L.marker([result.dropoff.lat, result.dropoff.lon]).addTo(map).bindPopup("Varış Noktası");

                distanceStat.textContent = result.distanceKm.toFixed(1) + " km";
                durationStat.textContent = Math.round(result.durationMinutes) + " dk";
                priceStat.textContent = result.estimatedPrice.toFixed(2) + " €";

                var routeBounds = bounds.concat(latLngs);
                map.fitBounds(routeBounds, { padding: [40, 40], maxZoom: 15 });
            })
            .catch(function () {
                setError("Rota hesaplanamadı, sunucuya ulaşılamadı.");
            });
    }

    loadRoute();

    var dashcamClose = document.getElementById("dashcamClose");
    var dashcamOverlay = document.getElementById("dashcamOverlay");
    if (dashcamClose && dashcamOverlay) {
        dashcamClose.addEventListener("click", function () {
            dashcamOverlay.classList.add("hidden");
        });
    }
})();
