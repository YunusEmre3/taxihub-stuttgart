(function () {
    // ---------- Topbar: hamburger + user dropdown ----------
    // Toggling a class on <body> (instead of hiding the sidebar directly)
    // lets the CSS collapse --sidebar-width everywhere at once, so the
    // topbar and main content reclaim the freed space instead of leaving
    // a blank gap where the sidebar used to be.
    var sidebarToggle = document.getElementById("sidebarToggle");
    if (sidebarToggle) {
        sidebarToggle.addEventListener("click", function () {
            document.body.classList.toggle("sidebar-collapsed");
        });
    }

    var userMenuToggle = document.getElementById("userMenuToggle");
    var userMenuDropdown = document.getElementById("userMenuDropdown");
    if (userMenuToggle && userMenuDropdown) {
        userMenuToggle.addEventListener("click", function (event) {
            event.stopPropagation();
            userMenuDropdown.classList.toggle("open");
        });
        document.addEventListener("click", function () {
            userMenuDropdown.classList.remove("open");
        });
    }

    // ---------- Admin: status filter + search over the bookings table ----------
    var statusFilter = document.getElementById("statusFilter");
    var searchBox = document.getElementById("searchBox");
    var tableRows = document.querySelectorAll(".data-table tbody tr");

    function applyTableFilters() {
        var status = statusFilter ? statusFilter.value : "ALL";
        var query = searchBox ? searchBox.value.trim().toLowerCase() : "";

        tableRows.forEach(function (row) {
            var matchesStatus = status === "ALL" || row.getAttribute("data-status") === status;
            var matchesSearch = query === "" || (row.getAttribute("data-search") || "").includes(query);
            row.classList.toggle("is-hidden", !(matchesStatus && matchesSearch));
        });
    }

    if (statusFilter) {
        statusFilter.addEventListener("change", applyTableFilters);
    }
    if (searchBox) {
        searchBox.addEventListener("input", applyTableFilters);
    }

    // ---------- Admin: booking detail modal ----------
    var detailModal = document.getElementById("detailModal");
    var modalMessage = document.getElementById("modalMessage");
    var modalSubtitle = document.getElementById("modalSubtitle");
    var modalCloseBtn = document.getElementById("modalCloseBtn");

    document.querySelectorAll(".btn-detail").forEach(function (button) {
        button.addEventListener("click", function () {
            modalMessage.textContent = button.getAttribute("data-message");
            modalSubtitle.textContent = button.getAttribute("data-name") + " — #" + button.getAttribute("data-code");
            detailModal.classList.add("open");
        });
    });

    if (modalCloseBtn) {
        modalCloseBtn.addEventListener("click", function () {
            detailModal.classList.remove("open");
        });
    }
    if (detailModal) {
        detailModal.addEventListener("click", function (event) {
            if (event.target === detailModal) {
                detailModal.classList.remove("open");
            }
        });
    }

    // ---------- Employee: Assign to Me / Complete Trip ----------
    var csrfToken = document.querySelector('meta[name="_csrf"]');
    var csrfHeader = document.querySelector('meta[name="_csrf_header"]');
    var errorToast = document.getElementById("errorToast");

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

    function postAction(url, button) {
        button.disabled = true;

        var headers = { "Content-Type": "application/json" };
        if (csrfToken && csrfHeader) {
            headers[csrfHeader.content] = csrfToken.content;
        }

        fetch(url, { method: "POST", headers: headers })
            .then(function (response) {
                return response.json().then(function (body) {
                    return { ok: response.ok, body: body };
                });
            })
            .then(function (result) {
                if (result.ok) {
                    window.location.reload();
                } else {
                    button.disabled = false;
                    showError(result.body.error || "Bir hata oluştu.");
                }
            })
            .catch(function () {
                button.disabled = false;
                showError("Sunucuya ulaşılamadı, tekrar deneyin.");
            });
    }

    document.querySelectorAll(".assign-btn").forEach(function (button) {
        button.addEventListener("click", function () {
            var bookingId = button.getAttribute("data-booking-id");
            postAction("/dashboard/bookings/" + bookingId + "/assign", button);
        });
    });

    var completeTripBtn = document.getElementById("completeTripBtn");
    if (completeTripBtn) {
        completeTripBtn.addEventListener("click", function () {
            var bookingId = completeTripBtn.getAttribute("data-booking-id");
            postAction("/dashboard/bookings/" + bookingId + "/complete", completeTripBtn);
        });
    }
})();
