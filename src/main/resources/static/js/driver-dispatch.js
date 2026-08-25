(function () {
    var STUTTGART_CENTER = [48.7758, 9.1829];

    var map = L.map("dispatchMap").setView(STUTTGART_CENTER, 12);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: "&copy; OpenStreetMap contributors",
        maxZoom: 19
    }).addTo(map);

    var csrfToken = document.querySelector('meta[name="_csrf"]');
    var csrfHeader = document.querySelector('meta[name="_csrf_header"]');
    var errorToast = document.getElementById("errorToast");

    var selectedBookingId = null;
    var bookingIcon = L.divIcon({ className: "", html: '<div class="dispatch-marker-booking"></div>', iconSize: [26, 26], iconAnchor: [13, 26] });
    var driverIcon = L.divIcon({ className: "", html: '<div class="dispatch-marker-driver">🚕</div>', iconSize: [30, 30], iconAnchor: [15, 15] });

    function showError(message) {
        if (!errorToast) {
            alert(message);
            return;
        }
        errorToast.textContent = message;
        errorToast.classList.add("open");
        setTimeout(function () {
            errorToast.classList.remove("open");
        }, 4000);
    }

    function assign(bookingId, employeeId) {
        var headers = { "Content-Type": "application/json" };
        if (csrfToken && csrfHeader) {
            headers[csrfHeader.content] = csrfToken.content;
        }

        fetch("/drivers/bookings/" + bookingId + "/assign/" + employeeId, { method: "POST", headers: headers })
            .then(function (response) {
                return response.json().then(function (body) {
                    return { ok: response.ok, body: body };
                });
            })
            .then(function (result) {
                if (result.ok) {
                    window.location.reload();
                } else {
                    showError(result.body.error || "Atama başarısız oldu.");
                }
            })
            .catch(function () {
                showError("Sunucuya ulaşılamadı, tekrar deneyin.");
            });
    }

    // ---------- Booking pins + side-list cards ----------

    var bookingCards = document.querySelectorAll(".booking-card");
    var bookingMarkerBounds = [];

    bookingCards.forEach(function (card) {
        var lat = parseFloat(card.getAttribute("data-lat"));
        var lng = parseFloat(card.getAttribute("data-lng"));
        var bookingId = card.getAttribute("data-booking-id");

        if (!isNaN(lat) && !isNaN(lng)) {
            var marker = L.marker([lat, lng], { icon: bookingIcon }).addTo(map);
            marker.bindPopup(
                "<strong>" + card.getAttribute("data-name") + "</strong>" +
                card.getAttribute("data-pickup") + "<br/>#" + card.getAttribute("data-code")
            );
            bookingMarkerBounds.push([lat, lng]);
        }

        card.addEventListener("dragstart", function (event) {
            event.dataTransfer.setData("text/plain", bookingId);
            card.classList.add("dragging");
        });
        card.addEventListener("dragend", function () {
            card.classList.remove("dragging");
        });

        card.addEventListener("click", function () {
            bookingCards.forEach(function (c) { c.classList.remove("selected"); });
            if (selectedBookingId === bookingId) {
                selectedBookingId = null;
            } else {
                selectedBookingId = bookingId;
                card.classList.add("selected");
            }
        });
    });

    // ---------- Driver markers + side-list rows ----------

    var driverRows = document.querySelectorAll(".driver-row");
    var driverMarkerBounds = [];

    function wireDropTarget(el, employeeId) {
        el.addEventListener("dragover", function (event) {
            event.preventDefault();
            el.classList.add("drop-target");
        });
        el.addEventListener("dragleave", function () {
            el.classList.remove("drop-target");
        });
        el.addEventListener("drop", function (event) {
            event.preventDefault();
            el.classList.remove("drop-target");
            var bookingId = event.dataTransfer.getData("text/plain");
            if (bookingId) {
                assign(bookingId, employeeId);
            }
        });
        el.addEventListener("click", function () {
            if (selectedBookingId) {
                assign(selectedBookingId, employeeId);
            }
        });
    }

    driverRows.forEach(function (row) {
        var lat = parseFloat(row.getAttribute("data-lat"));
        var lng = parseFloat(row.getAttribute("data-lng"));
        var employeeId = row.getAttribute("data-employee-id");

        wireDropTarget(row, employeeId);

        if (!isNaN(lat) && !isNaN(lng)) {
            var marker = L.marker([lat, lng], { icon: driverIcon }).addTo(map);
            marker.bindPopup("<strong>" + row.getAttribute("data-name") + "</strong>Boşta");
            driverMarkerBounds.push([lat, lng]);

            marker.on("add", function () {
                wireDropTarget(marker.getElement(), employeeId);
            });
        }
    });

    var allBounds = bookingMarkerBounds.concat(driverMarkerBounds);
    if (allBounds.length > 0) {
        map.fitBounds(allBounds, { padding: [40, 40], maxZoom: 14 });
    }
})();
