/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Torsten Jaeschke
 */

var projectAPiUrl = "/api/v2/project";
var quickstarterAPIUrl = "api/v1/quickstarter";
var quickstarterCount = 1;
var quickStarters;
var nameCompare = {};
var validatorOptions = {
  custom: {
    unique: function (el) {
      var newCompId = el.val().trim();
      var name = el.attr("name");
      return !isUniqueComponentId(newCompId, name)
    }
  },
  errors: {
    unique: "This component already exist"
  }
};

$(document).ready(function(){
   $('form#createProject').validator(validatorOptions);
   $('form#modifyProject').validator(validatorOptions);


  // inital getting of quickstarters for later use
  $.get(quickstarterAPIUrl, function (data) {
    console.log(data);
    quickStarters = data;
  }, "json");

  // multiple field adding
  $(document).on('click', '.btn-add', function(e) {
    e.preventDefault();
    var currentform = $(this).closest('form')[0];

    var controlForm = $(currentform).find('.quickstartergroup'),
        currentEntry = $(this).parents('.entry:first'),
        clonedEntry = currentEntry.clone();

    quickstarterCount++;

    clonedEntry.find("input[type='checkbox']").replaceWith(clonedEntry.find("input[type='checkbox']").attr('name', 'quickstart-checked-'+quickstarterCount).prop("checked", false).prop("disabled", true));
    clonedEntry.find("[name^='quickstart-type']").replaceWith(clonedEntry.find("[name^='quickstart-type']").attr('name', 'quickstart-type-'+quickstarterCount));
    clonedEntry.find("[name^='quickstart-comp']").replaceWith(clonedEntry.find("[name^='quickstart-comp']").attr('name', 'quickstart-comp-id-'+quickstarterCount).val('').prop("required", false));
    clonedEntry.find("[name^='quickstart-error']").replaceWith(clonedEntry.find("[name^='quickstart-error']").attr('name', 'quickstart-error-'+quickstarterCount).html(''));

    var newEntry = $(clonedEntry).appendTo(controlForm);

    controlForm.find('.entry:not(:last) .btn-add')
    .removeClass('btn-add').addClass('btn-remove')
    .removeClass('btn-success').addClass('btn-danger')
    .html('<span class="glyphicon glyphicon-minus"></span>');
    $(currentform).validator('update');
  }).on('click', '.btn-remove', function(e) {
    var currentform = $(this).closest('form')[0];
    var comp_name = $(this).parents('.entry:first').find("[name^='quickstart-comp']").attr("name");
    delete nameCompare[comp_name];
    $(this).parents('.entry:first').remove();
    $(currentform).validator('update')
    e.preventDefault();
    return false;
  });

  /**
   * event handlers for the mode option, if you want to create or modif a project
   */
  $("#optNewProject").click(function() {
    $( "form#createProject" ).removeClass("hidden");
    $( "form#modifyProject" ).addClass("hidden");
    nameCompare = {};
  });

  $("#optExistingInitiave").click(function() {
    $( "form#modifyProject" ).removeClass("hidden");
    $( "form#createProject" ).addClass("hidden");
    nameCompare = {};
  });

  $("#resButton").click(function() {
	  var value = $("#resButton").attr('name');
	  console.log("button clicked: " +  value);
	  if (value == "success") 
	  {
		console.log("reloading ... ");
		location.reload(true);
	  } else 
	  {
		$("#resButton").attr('name', 'result');  
	  }
  });
		  
  $("#specialPermissionSet").change(function() {
    if ( $( "#specialPermissionSet" )[0] .checked)
    {
        $( "#urgroupdiv" ).removeClass("hidden");
        $( "#ugroupdiv" ).removeClass("hidden");
        $( "#agroupdiv" ).removeClass("hidden");
        $( "#auserdiv" ).removeClass("hidden");

        $( "#adminGroup" ).prop("required", true);
        $( "#readonlyGroup" ).prop("required", true);
        $( "#userGroup" ).prop("required", true);
        
        $( "#admin" ).focus();
    } else {
        $( "#urgroupdiv" ).addClass("hidden");    	
        $( "#ugroupdiv" ).addClass("hidden");    	
        $( "#agroupdiv" ).addClass("hidden");
        
        $( "#adminGroup" ).prop("required", false);
        $( "#readonlyGroup" ).prop("required", false);
        $( "#userGroup" ).prop("required", false);
        
        $( "#auserdiv" ).addClass("hidden");    	
    }
    nameCompare = {};
  });  

  
  /**
   * This is the event handler for the quickstarter select box
   */
  $(document).on("change", ".quickstart-chooser", function () {
    var currentform = $(this).closest('form')[0];
    var indexOfSign = ($(this).attr("name")).lastIndexOf("-");
    var id = ($(this).attr("name")).substr(indexOfSign+1);
    if($(this).val() != "") {
      console.log($(this).attr("name"));
      for(var obj in quickStarters) {
        if($(this).val() == quickStarters[obj].id) {
          var str = quickStarters[obj].name;
          $(currentform).find("[name='quickstart-checked-"+id+"']").prop('disabled', false);
          $(currentform).find("[name='quickstart-checked-"+id+"']").prop('checked', true);
          var compId = str.replace(new RegExp("_", 'g'), "-");
          $(currentform).find("[name='quickstart-comp-id-"+id+"']").val(compId).prop("required", true).attr("data-unique", "unique");
          nameCompare['quickstart-comp-id-'+id] = compId;
          $(currentform).validator('update');
        }
      }
    } else {
      $(currentform).find("[name='quickstart-checked-"+id+"']").prop('disabled', true);
      $(currentform).find("[name='quickstart-checked-"+id+"']").prop('checked', false);
      $(currentform).find("[name='quickstart-comp-id-"+id+"']").prop("required", false).removeAttr("data-unique");
      delete nameCompare['quickstart-comp-id-'+id];
      $(currentform).validator('update');
    }
  });

  /*
  This event handler is used if you modify an existing project and reacts to changes in the project list
  */
  $("#projects").change(function () {
    nameCompare = {};
    $( "#quickstartTable" ).addClass("hidden");
    $("#modifyProject input[type='checkbox'][name^='quickstart']").each(function() {
        $(this).prop('checked', false);
    });
    if($(this).val() != "") {
      $("#modifySubmit").removeClass("disable");
      $("#modifySubmit").prop("disabled", false);
      var url= projectAPiUrl+'/'+$(this).val();
      $.get(url, function (data) {
          console.log(data)
        if(data != null) {
          if(data.quickstarters != null) {
            html = "";
            for(var obj in data.quickstarters) {
              html+="<tr>";
              html+="<td>"+data.quickstarters[obj].component_description+"</td>";
              html+="<td>"+data.quickstarters[obj].component_id+"</td>";
              html+="<tr>";
              nameCompare[data.quickstarters[obj].component_type] = data.quickstarters[obj].component_id;
            }
            console.log(html)
            $("#quickstartRow").html(html);
            $( "#quickstartTable" ).removeClass("hidden");
          }
          
          // tick checkbox based on project created true false
          $('#jiraconfluencespaceInfo').prop('checked',data.bugtrackerSpace);

          // tick checkbox based on project created true false
          $('#openshiftProjectInfo').prop('checked',data.platformRuntime);
          // only allow upgrade if modifyable == true
          if (data.platformRuntime == false && $('#openshiftProjectInfo').prop('modifyable') == true) {
        	  // allow people to change it - update will take care about this
        	  $("#openshiftProjectInfo").removeClass("disable");
        	  $("#openshiftProjectInfo").prop("disabled", false);
          }
          
          // set description
          $('#projectDescription').val(data.description);
          $( "#infoProjectTable" ).removeClass("hidden");
        }
      }, "json");
    }
  });

  /**
   * logic for the create from submit
   */
  $( "form#createProject" ).submit(function( event ) {
    if (event.isDefaultPrevented()) {
      // handle the invalid form...
    } else {

      var formdata = $(this).serializeArray();
      var projectData = JSON.stringify(objectifyForm( $(this).serializeArray()));

      $("#projectData").addClass("hide");
      $("#createModal").modal("show");

      $("#createProject").hide();

      $("#resProject")
      .text("Project provision in progress")
      .removeClass("alert-danger")
      .removeClass("alert-success");
      $("#resButton").button("loading");

      $.ajax({
        url:projectAPiUrl,
        type:"POST",
        data:projectData,
        contentType:"application/json; charset=utf-8",
        dataType:"json",
        timeout: 480000,
        success: function(data, status, xhr){

          summarize(data);

          $("#resProject")
          .addClass("alert-success")
          .text("Project successful created.");
          console.log("successfully created...");
          console.log( data );
          
          // add success flag, this way we know when to reload.
          $("#resButton").attr('name', 'success');
          $("#resButton").button("reset");
          $("#createProject").trigger("reset");
          $("#createProject").show();
        },
        error:  function(data, status, xhr){
            console.log("fail");
            console.log( data );
          $("#resProject")
          .addClass("alert-danger")
          .text("Could not create project: " + data.responseText);
          $("#resButton").button("reset");
          $("#createProject").show();
        }
      });
    }

    event.preventDefault();
  });

  /**
   * logic for the modify project form submit
   */
  $( "form#modifyProject" ).submit(function( event ) {
    if (event.isDefaultPrevented()) {
      // handle the invalid form...
    } else {

      var projectData = JSON.stringify(objectifyForm( $(this).serializeArray()));

      $("#projectData").addClass("hide");
      $("#createModal").modal("show");

      $("#modifyProject").hide();

      $("#resProject")
      .text("Project provision in progress")
      .removeClass("alert-danger")
      .removeClass("alert-success");
      $("#resButton").button("loading");

      $.ajax({
        url:projectAPiUrl,
        type:"PUT",
        data:projectData,
        contentType:"application/json; charset=utf-8",
        dataType:"json",
        success: function(data, status, xhr){
          summarize(data);
          $("#resProject")
          .addClass("alert-success")
          .text("Project successfully updated.");
          $("#resButton").button("reset");
          $("#modifyProject").trigger("reset");
          $( "#quickstartTable" ).addClass("hidden");
          $("#modifyProject").show();
          //startProvision(data);
        },
        error:  function(data, status, xhr){
            console.log("fail");
            console.log( data );
          $("#resProject")
          .addClass("alert-danger")
          .text("Can not update project, error " + data.responseText);
          $("#resButton").button("reset");
          $("#modifyProject").show();
        }
      });

    }

    event.preventDefault();
  });

  //automatic key generation
  $(document).on('keyup', "#projectName", function () {
    if($("#projectKey").val().trim() == "") {
      clearTimeout($(this).data('timeout'));
      var _self = this;
      $(this).data('timeout', setTimeout(function () {

        $.get(projectAPiUrl+'/key/generate', {
          name: _self.value
        }, function (data) {
            $("#projectKey").val(data.projectKey);
        }, "json");

      }, 1000));
    }
  });

  //transform input to uppercase
  $(document).on('keyup', "#projectKey", function () {
    if($("#projectKey").val().trim() != "") {
      var val = $("#projectKey").val().trim();
      $("#projectKey").val(val.toUpperCase());
    }
  });


});

//proof if name is unique
function isUniqueComponentId(newCompId,elName) {
  var retValue = true;
  nameCompare[elName] = newCompId;
  for(var obj in nameCompare) {
    if(obj != elName) {
      if (nameCompare[obj] == newCompId) {
        retValue = false;
      }
    }
  }
  return retValue;
}

//add summarization to modal html
function summarize(data) {
  $("#dataProjectName").html(data.projectName);
  $("#dataProjectKey").html(data.projectKey);
  
  $("#dataJiraConfluenceCreated").text(data.bugtrackerSpace);
  
  // this is for the default case where spaces should be created.
  if (data.bugtrackerSpace) 
  {
	  $("#dataJiraUrl").html("<a href='" + data.bugtrackerUrl+"' target='_blank'>" + data.bugtrackerUrl +"</a>");
	  $("#dataConfluenceUrl").html("<a href='" + data.collaborationSpaceUrl+"' target='_blank'>" + data.collaborationSpaceUrl +"</a>");
  } else 
  {
  	  $("#dataJiraUrlDiv").hide();
  	  $("#dataConfluenceUrlDiv").hide();
  }

  if (data.lastExecutionJobs != null) {
	 console.log("jobs found: " + data.lastExecutionJobs + " length: " + data.lastExecutionJobs.length);
     html = "";
     if (data.lastExecutionJobs.length == 0) {
		 html += "<a href='" + data.lastExecutionJobs +"' target='_blank'>" + data.lastExecutionJobs +"</a></br>";
     } 
     else {
    	 for (var jobId = 0; jobId < data.lastExecutionJobs.length; jobId++) {
    		 var jobUrl = data.lastExecutionJobs[jobId];
    		 html += "<a href='" + jobUrl +"' target='_blank'>" + jobUrl +"</a></br>";
    		 console.log(html)
    	 } 
     }
	 
	 $("#dataJobUrls").html(html);
  }

  if (data.platformRuntime) 
  {
	  // this was moved, in case of jira / confluence only - no more bitbucket
	  $("#dataBitbucketUrl").html("<a href='" + data.scmvcsUrl+"' target='_blank'>" + data.scmvcsUrl +"</a>");
	  $("#dataJenkinsUrl").html("<a href='" + data.platformBuildEngineUrl + "' target='_blank'>" + data.platformBuildEngineUrl +"</a>");
  } else 
  {
  	  $("#dataJenkinsUrlDiv").hide();
  	  $("#dataBitbucketUrlDiv").hide();
  }  
  
  $("#projectData").removeClass("hide");

}

//create project data json for ajax calls to api
function objectifyForm(formArray) {//serialize data function
  var returnArray = {};
  var array = {};
  var quickstartArray = {};
  var inputName = "";
  //  lousy html only a checked checkbox is returned - baehh
  var createJiraConfluenceSpace = false;
  var createOpenshiftproject = false;
  var createPermissionset = false;

  for (var i = 0; i < formArray.length; i++){
      if((formArray[i]['name']).startsWith("quickstart")) {
        var indexOfSign = (formArray[i]['name']).lastIndexOf("-");
        var id = (formArray[i]['name']).substr(indexOfSign+1);
        if(typeof quickstartArray[id] != "object") {
          quickstartArray[id] = {};
        }
        if((formArray[i]['name']).startsWith("quickstart-type")) {
          quickstartArray[id]["component_type"] = formArray[i]['value'];
        }
        if((formArray[i]['name']).startsWith("quickstart-comp")) {
          quickstartArray[id]["component_id"] = formArray[i]['value'];
        }
        if((formArray[i]['name']).startsWith("quickstart-checked")) {
          quickstartArray[id]["checked"] = formArray[i]['value'];
        }
        //quickstartArray[i].push(formArray[i]);
      } else if ((formArray[i]['name']).startsWith("bugtrackerSpace")) {
        // this is only the case if checkbox checked
        createJiraConfluenceSpace = true;
      } else if ((formArray[i]['name']).startsWith("platformRuntime")) {
        // this is only the case if checkbox checked
    	createOpenshiftproject = true;
      } else if ((formArray[i]['name']).startsWith("specialPermissionSet")) {
          // this is only the case if checkbox checked
    	createPermissionset = true;
      } else {
        returnArray[formArray[i]['name']] = formArray[i]['value'];
      }
  }
  
  // add jira confluence space and openshift project creation trigger
  returnArray["bugtrackerSpace"] = createJiraConfluenceSpace;
  returnArray["platformRuntime"] = createOpenshiftproject;
  returnArray["specialPermissionSet"] = createPermissionset;
  
  if(inputName != "") {
    returnArray[inputName] = array;
  }
  
  returnArray["quickstarters"] = [];
  for (var prop in quickstartArray) {
    if(quickstartArray[prop].checked != null) {
      delete quickstartArray[prop].checked;
      if(quickstartArray[prop].component_id != "" && quickstartArray[prop].component_type != "") {
        returnArray["quickstarters"].push(quickstartArray[prop]);
      }
    }
  }
  return returnArray;
}

