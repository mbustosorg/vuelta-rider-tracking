$(document).ready(function() {
    var loadingTimestamp = 0;
    var latitude = 0.0;
    var longitude = 0.0;
    $('#bibNumber').on('submit', handleBibNumberSelect);
    $('#bibNumber').keypress(function(event) {
        if (event.which == 13) {
            event.preventDefault();
            $('#bibNumber').submit();
        }
    });

    function setLocation(position) {
        latitude = position.coords.latitude;
        longitude = position.coords.longitude;
        $('#location').html('<strong>Lat: ' + latitude.toFixed(3) + ' Lon: ' + longitude.toFixed(3) + "</strong>")
    };

    function getLocation() {
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(setLocation);
        } else {
            alert("Geolocation is not supported by this browser.");
        }
    };

    function handleBibNumberSelect() {
        var bibNumber = $('#bibNumber').val();
        if (isNaN(bibNumber)) {
            $('#submitResult').addClass('alert-danger');
            $('#submitResult').removeClass('alert-success');
            $('#submitResult').html('<strong>   Error: </strong> Bib number is not numeric');
        } else if (latitude == 0.0 || longitude == 0.0) {
            $('#submitResult').addClass('alert-danger');
            $('#submitResult').removeClass('alert-success');
            $('#submitResult').html('<strong>   Error: </strong> Lat / Lon not set, please wait');
        } else {
            loadingTimestamp = Date.now();
            $('#runningQuery').removeClass('hide');
            getLocation();
            var dataString = "{\"bibNumber\": " + bibNumber + ", \"latitude\":" + latitude.toString() + ", \"longitude\": " + longitude.toString() + "}";
            $.ajax({
                type: "POST",
                url: '/updateRider/' + bibNumber,
                dataType: "json",
                data: dataString,
                cache: false
            }).done(function(results) {
                chartData(results, bibNumber)
            });
        }
    };

    function chartData(results, bibNumber) {
        $('#runningQuery').addClass('hide');
        if (results.rider.bibNumber == 0) {
            $('#submitResult').addClass('alert-danger');
            $('#submitResult').removeClass('alert-success');
            $('#submitResult').html('<strong>   Error: </strong> Could not find bib number '+ bibNumber)
        } else {
            $('#submitResult').addClass('alert-success');
            $('#submitResult').removeClass('alert-danger');
            $('#submitResult').html('<strong>   Submitted </strong> for '+ results.rider.name)
        }
    };

    getLocation();
});