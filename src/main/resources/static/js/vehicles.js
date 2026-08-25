(function () {
    // ---------- Search + status/type filters over the vehicle table ----------
    var searchBox = document.getElementById("vehicleSearchBox");
    var statusFilter = document.getElementById("vehicleStatusFilter");
    var typeFilter = document.getElementById("vehicleTypeFilter");
    var rows = document.querySelectorAll(".data-table tbody tr");

    function applyFilters() {
        var query = searchBox ? searchBox.value.trim().toLowerCase() : "";
        var status = statusFilter ? statusFilter.value : "ALL";
        var type = typeFilter ? typeFilter.value : "ALL";

        rows.forEach(function (row) {
            var matchesStatus = status === "ALL" || row.getAttribute("data-status") === status;
            var matchesType = type === "ALL" || row.getAttribute("data-vehicle-type") === type;
            var matchesSearch = query === "" || (row.getAttribute("data-search") || "").includes(query);
            row.classList.toggle("is-hidden", !(matchesStatus && matchesType && matchesSearch));
        });
    }

    if (searchBox) {
        searchBox.addEventListener("input", applyFilters);
    }
    if (statusFilter) {
        statusFilter.addEventListener("change", applyFilters);
    }
    if (typeFilter) {
        typeFilter.addEventListener("change", applyFilters);
    }

    // ---------- "Add New Vehicle" modal ----------
    var addModal = document.getElementById("addVehicleModal");
    var openBtn = document.getElementById("openAddVehicleBtn");
    var closeBtn = document.getElementById("addVehicleCloseBtn");

    if (openBtn && addModal) {
        openBtn.addEventListener("click", function () {
            addModal.classList.add("open");
        });
    }
    if (closeBtn && addModal) {
        closeBtn.addEventListener("click", function () {
            addModal.classList.remove("open");
        });
    }
    if (addModal) {
        addModal.addEventListener("click", function (event) {
            if (event.target === addModal) {
                addModal.classList.remove("open");
            }
        });
    }
})();
