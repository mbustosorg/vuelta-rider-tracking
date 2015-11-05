$(document).ready(function() {
  $("form#loginForm").submit(function() {
    var bibNumber = $('#inputBibNumber').val();
    var name = $('#inputName').val();
    if (bibNumber && name) {
      var http = location.protocol;
	  var slashes = http.concat("//");
	  var host = slashes.concat(window.location.host);
      $.ajax({
        type: "POST",
        url: "/admin",
        contentType: "application/x-www-form-urlencoded; charset=utf-8",
        data: "inputBibNumber=" + bibNumber + "&inputName=" + name,
        error: function(XMLHttpRequest, textStatus, errorThrown) {
          $('#authenticationAlert').text(XMLHttpRequest.responseText);
          $('#authenticationAlert').removeClass('hide');
        },
        success: function(data){
          if (data.error) {
          }
          else {
			var redirect = host.concat(data);
            window.location.href=redirect;
          }
        }
      });
    }
    return false;
  });
});