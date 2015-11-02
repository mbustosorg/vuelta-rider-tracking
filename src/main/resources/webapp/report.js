$(document).ready(function() {
    function handleReportSelect() {
        $.ajax({
            type: "GET",
            url: '/restStopCounts',
            cache: false
        }).done(function(results) {
            chartData(results)
        });
		$.ajax({
			url: '/riderStatus',
			cache: false
		}).done (function (riderStatus) {
			$('tbody#riderstatus_table_body').empty();
			$.each(riderStatus, function(key, currentStatus) {
				$('#riderstatus_table_body').append('<tr>' +
					'<td>' + currentStatus.bibNumber + '</td>' +
					'<td>' + currentStatus.name + '</td>' +
					'<td>' + currentStatus.stop + '</td>' +
					'<td>' + currentStatus.timestamp + '</td>' +
					'</tr>'
				);
			});
		});
        setTimeout(handleReportSelect,10000);
    };

    function chartData(results) {
      var data = google.visualization.arrayToDataTable([
        ['Stop', 'Count'],
        [results[0][0], results[0][1]],
        [results[1][0], results[1][1]],
        [results[2][0], results[2][1]],
        [results[3][0], results[3][1]],
        [results[4][0], results[4][1]]
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
});