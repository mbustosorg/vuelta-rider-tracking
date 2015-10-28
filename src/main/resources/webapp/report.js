$(document).ready(function() {
    function handleReportSelect() {
        $.ajax({
            type: "GET",
            url: '/restStopCounts',
            cache: false
        }).done(function(results) {
            chartData(results)
        });
    };

    function chartData(results) {
      var data = google.visualization.arrayToDataTable([
        ['Stop', 'Count'],
        [results[0][0].name, results[0][1]],
        [results[1][0].name, results[1][1]],
        [results[2][0].name, results[2][1]],
        [results[3][0].name, results[3][1]],
        [results[4][0].name, results[4][1]]
      ]);

      var options = {
        title: 'Rider count by last rest stop',
        hAxis: {
          title: 'Stop'
        },
        vAxis: {
          title: 'Count',
          minValue: 0
        }
      };
      var chart = new google.visualization.ColumnChart(document.getElementById('chart_div'));
      chart.draw(data, options);
    };

    handleReportSelect();
    setTimeout(handleReportSelect,10000);
});