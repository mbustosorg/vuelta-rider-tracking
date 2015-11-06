$(document).ready(function() {
  $('#addRider').on('click', handleAddRider);
  $('#updateRider').on('click', handleUpdateRider);
  $('#deleteRider').on('click', handleDeleteRider);
  $('#retrieveRider').on('click', handleRetrieveRider);

  function handleAddRider() {
    var bibNumber = $('#inputBibNumber').val();
    var name = $('#inputName').val();
    if (bibNumber && name) {
      $.ajax({
        type: "POST",
        url: "/rider/" + bibNumber,
        contentType: "application/x-www-form-urlencoded; charset=utf-8",
        data: "name=" + name,
        success: function(data){
           if (data.bibNumber == 0) {
              $('#authenticationAlert').addClass('alert-danger');
              $('#authenticationAlert').removeClass('alert-success');
              $('#authenticationAlert').html('<strong>   Error: </strong> Could not create rider');
           } else {
              $('#authenticationAlert').addClass('alert-success');
              $('#authenticationAlert').removeClass('alert-danger');
              $('#authenticationAlert').html('<strong>   Success: </strong> Rider created');
           }
        }
      });
  };

  function handleUpdateRider() {
  };

  function handleDeleteRider() {
    var bibNumber = $('#inputBibNumber').val();
    if (isNaN(bibNumber) || !bibNumber) {
      $('#authenticationAlert').addClass('alert-danger');
      $('#authenticationAlert').removeClass('alert-success');
      $('#authenticationAlert').html('<strong>   Error: </strong> Bib number is not numeric');
    } else {
      $.ajax({
        type: "POST",
        url: "/rider/" + bibNumber + "/delete",
        error: function(XMLHttpRequest, textStatus, errorThrown) {
          $('#authenticationAlert').text(XMLHttpRequest.responseText);
          $('#authenticationAlert').removeClass('hide');
        },
        success: function(data){
           $('#authenticationAlert').addClass('alert-success');
           $('#authenticationAlert').removeClass('alert-danger');
           $('#authenticationAlert').html('<strong>   Success: </strong> Bib number deleted');
        }
      });
    }
    return false;
  }

  function handleRetrieveRider() {
    var bibNumber = $('#inputBibNumber').val();
    if (isNaN(bibNumber) || !bibNumber) {
      $('#authenticationAlert').addClass('alert-danger');
      $('#authenticationAlert').removeClass('alert-success');
      $('#authenticationAlert').html('<strong>   Error: </strong> Bib number is not numeric');
    } else {
      $.ajax({
        type: "GET",
        url: "/rider/" + bibNumber,
        error: function(XMLHttpRequest, textStatus, errorThrown) {
          $('#authenticationAlert').addClass('alert-danger');
          $('#authenticationAlert').removeClass('alert-success');
          $('#authenticationAlert').text(XMLHttpRequest.responseText);
          $('#authenticationAlert').removeClass('hide');
        },
        success: function(data){
           if (data.bibNumber == 0) {
              $('#authenticationAlert').addClass('alert-danger');
              $('#authenticationAlert').removeClass('alert-success');
              $('#authenticationAlert').html('<strong>   Error: </strong> Bib number not found');
           } else {
              $('#inputName').val(data.name);
              $('#authenticationAlert').addClass('alert-success');
              $('#authenticationAlert').removeClass('alert-danger');
              $('#authenticationAlert').html('<strong>   Success: </strong> Bib number retrieved');
           }
        }
      });
    }
    return false;
  }

});