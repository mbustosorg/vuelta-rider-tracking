$(document).ready(function() {
  $("form#loginForm").submit(function() {
    var username = $('#inputEmail').val();
    var password = $('#inputPassword').val();
    var redirect = '';
    $("#loggingIn").removeClass('hide');
    if (username && password) {
      var http = location.protocol;
	  var slashes = http.concat("//");
	  var host = slashes.concat(window.location.host);
      $.ajax({
        type: "POST",
        url: "/login",
        contentType: "application/x-www-form-urlencoded; charset=utf-8",
        data: "inputEmail=" + username + "&inputPassword=" + password,
        error: function(XMLHttpRequest, textStatus, errorThrown) {
          $('#authenticationAlert').text(XMLHttpRequest.responseText);
          $('#authenticationAlert').removeClass('hide');
	      $("#loggingIn").addClass('hide');
        },
        success: function(data){
          if (data.error) {
          }
          else {
			redirect = host.concat(data);
            window.location.replace(data);
          }
	      $("#loggingIn").addClass('hide');
        }
      });
    }
    return false;
  });
});