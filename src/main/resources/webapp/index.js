$(document).ready(function() {
    var loadingTimestamp = 0;
    var latitude = 0.0;
    var longitude = 0.0;
    $('#bibNumber').on('submit', handleBibNumberSelect);
    $('#bibNumber').keypress(function(event) {
        if (event.which == 13) {
            event.preventDefault();
            $("#bibNumber").submit();
        }
    });

    function setLocation(position) {
        latitude = position.coords.latitude;
        longitude = position.coords.longitude;
        $('#location').text('Your location - Lat: ' + latitude.toFixed(3) + ' Longitude: ' + longitude.toFixed(3))
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
        if (!isNan(bibNumber)) {
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
            }).done(function(records) {
                chartData(records)
            });
        }
    };

    function chartData(records) {
        $('#runningQuery').addClass('hide');
    /*
        lastQueryTimeseries = timeSeries;
        var queryTotalTime = (Date.now() - loadingTimestamp) / 1000.0;
        $('#resultSummary').text(records.records.length.toString() + " records in " + queryTotalTime.toString() + " s ");
        var value_1 = [];
        var variableNames = $('#variablesText').val().split(',');
        var cxrfNames = $('#companyText').val().split(',');
        if (timeSeries) value_1[0] = ['Month'];
        else value_1[0] = ['cxrf'];
        if (timeSeries) {
            for (i = 0; i < variableNames.length; i++) {
                for (j = 0; j < cxrfNames.length; j++) {
                    value_1[0][i * cxrfNames.length + j + 1] = variableNames[i] + '_' + cxrfNames[j].toString();
                }
            }
        } else {
            for (i = 0; i < variableNames.length; i++) {
                value_1[0][i + 1] = variableNames[i];
            }
        }
        var rowMap = {};
        var rowCounter = 1;
        for (i = 1; i < records.records.length + 1; i++) {
            if (i < records.records.length) {
                 var julianMonth = records.records[i - 1].month;
                 var tableIndex = new Date(julianMonth / 12 + 1950, julianMonth % 12 + 1, 1);
                 var rowIndex = i;
                 if (timeSeries) {
                     if (typeof rowMap[julianMonth] === "undefined") {
                         rowMap[julianMonth] = rowCounter;
                         rowCounter++;
                     }
                    rowIndex = rowMap[julianMonth];
                    tableIndex.setMonth(tableIndex.getMonth() + 1, 0, 23, 59, 59, 0);
                 } else {
                    tableIndex = i.toString();
                 }
                 if (typeof value_1[rowIndex] === "undefined") {
                    if (timeSeries) value_1[rowIndex] = new Array(cxrfNames.length * variableNames.length + 1);
                    else value_1[rowIndex] = new Array(variableNames.length + 1);
                    value_1[rowIndex][0] = tableIndex;
                 }
                 for (j = 0; j < variableNames.length; j++) {
                     var cxrfIndex = cxrfNames.indexOf(records.records[i - 1].cxrf.toString());
                     var column = 0;
                     if (timeSeries) column = j * cxrfNames.length + cxrfIndex + 1;
                     else column = j + 1;
                     if (typeof records.records[i - 1].variables[variableNames[j]] === "undefined") {
                        value_1[rowIndex][column] = null;
                     } else {
                        value_1[rowIndex][column] = records.records[i - 1].variables[variableNames[j]].value;
                     }
                     if (rowIndex == 1) value_1[rowIndex][column] = 0.0;
                 }
            }
        }
        var data = google.visualization.arrayToDataTable(value_1);
        var chartOptions = {
            legend: {
                position: 'bottom'
            }
        };
        var annotationChartOptions = {
        };

        //var chart = new google.visualization.LineChart(document.getElementById('chart_value_1'));
        //chart.draw(data, chartOptions);
        if (timeSeries) {
            var annotationChart = new google.visualization.AnnotationChart(document.getElementById('chart_div'));
            annotationChart.draw(data, annotationChartOptions);
        } else {
            var histogramChart = new google.visualization.Histogram(document.getElementById('chart_div'));
            var crop = [];
            for(var i = 0; i < value_1.length; i++){
                crop[i] = value_1[i].slice(1, value_1[i].length);
            }
            histogramChart.draw(google.visualization.arrayToDataTable(crop), chartOptions);
        }
        $('#runningQuery').addClass('hide');
        */
    };
    getLocation();
});