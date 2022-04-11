/*
  0. '/ad' page is loaded within amazon turk using an iframe (as externalURL questioneer)
  1. do nothing with eg url like '/ad' -- show task description for preview
  2. parent page accepts HIT and redirects iframe calling this js file to e.g. /ad?workerId=x&workerId=y&hitId=z
    1. when url has extra prams: show consent button
    2. when consent clicked, (async) look up hitId's arguments (anchor permutations of task settings)
    3. show play button and completion code form
    4. when clicked, popup task
      1. run through task
      2. task finishes calls back to this window ('caller') to fill out the form and automaticly submit
      3. submit calls to externalSubmit which loads js to call outside the iframe ('parent')
    5. parent frame submits completion code to amazon
    6. MANUAL confirm or reject worker. autoaccepted after 30 days

*/

// ugly hack to store popup window for testing
function task_popup_window(win){
   if(win!==undefined) { task_popup_window.win = win; console.log(win)}
   return(task_popup_window.win || null);
}

async function get_anchor(task) {
  const resp = await fetch("/anchor/"+task);
  if(!resp.ok){return "";} // when no server or server error
  const ret = await resp.json()
  return ret.anchor;
}
function url_params(){
    const url = new URL(document.URL);

    // id and task from mturk.
    // TODO: should hitId have a lookup in psiclj?
    params = {"id": url.searchParams.get("workerId"),
              "timepoint": url.searchParams.get("assignmentId"),
              "task": url.searchParams.get("hitId"),
              "hash": url.hash,
              "anchor": "vanilla", //populate from /anchor/$hitId
              "external": "https://www.mturk.com/mturk/externalSubmit" };

    //if preview. dont show the "PLAY" link
    if(!params["id"] ||
       params["id"] === "ASSIGNMENT_ID_NOT_AVAILABLE"){
        return(undefined);
    }

    // redirect to sandbox if that's anywhere in the url
    // NB!! if some task parameter uses the word sandbox, this will cause some pain
    if( document.URL.indexOf("sandbox") >= 0 ||
        (document.parent?(document.parent.URL.indexOf("sandbox")>=0):false)) {
     params["external"] = "https://workersandbox.mturk.com/mturk/externalSubmit"
    }

    //maybe something went wrong
    //should we bail instead of displaying the "PLAY" link?
    if(!params["task"]){params["task"] = "no_task";}
    if(!params["timepoint"]){params["timepoint"] = 1;}

    return(params)
}

function params_to_taskurl(params){
    return("/" + params.id + "/" + params.task + "/" + params.timepoint + "/" + 1 + "/");
}

function popup_nofrills(url, w, h) {
   //window.alert("popup for "+url)
   const win = window.open(url,"Popup",
                "toolbar=no,location=no,status=no,menubar=no,"+
                "scrollbars=no,width="+w+",height="+h);//,resizable=no
   return(win);
}
function change_to_play_button(popup_cmd, action, id){
   //NB. <div#buttons> created by add_consent
   obj = document.querySelector("#buttons")
   if(obj == null){ obj = document.body; }
   obj.innerHTML =
        "<a class='clickme' href='#' onClick=\""+popup_cmd+"\">PLAY!</a>"+
        "<br><br><form method=post action='"+ action +"'>"+
        "Completion code: <input size=5 id=completecode value='' name='completecode'><br>"+
        "<input type=hidden name=assignmentId value='"+id+"'>" +
        "<input id=mtruksubmit type='submit' value='COMPLETE'></form>";

}

// adds a link to the task if assignmentId is reasonable
// see taskCompleteCode for auto submitting
async function append_play_link(){
   params = url_params()
   if(!params){return;}
   params['anchor'] = await get_anchor(params['task'])
   url = params_to_taskurl(params)
   console.log(params)
   anchors = (params.anchor!==undefined?params.anchor:"")  // + "&noinstruction&fewtrials"
   cmd = "popup_nofrills('" + url +"#"+ anchors +"', 1024, 768)"
   change_to_play_button(cmd, params.external, params.timepoint)
}


/**** START HERE: use in onload ****/
function add_consent(){
   // only show consent when we have workerId,assignmentId, and hitID in URL
   params = url_params();
   if(!params){return;}

   //NB. <div#buttons> used by append_play_link (otherwise replaces body)
   let preview = document.querySelector("div#preview")
   let consent = document.querySelector("div#accept")
   if(preview !== null && consent !== null)  {
      preview.style.display = "none";
      consent.style.display = "block";
   } else {
      document.body.innerHTML =
        'You must <ol><li>Click "Read Consent" to read the pop up and confirm your participation.</li>'+
        '<li> Then the "Play" button will appear. Click to play in a new window!</li>' +
        '<li> When you finish, you will be given a completion code to enter back here.</li></ol>' +
        "<div id='buttons'> <a class='clickme' href='#' onClick='consent_form_clicked()'>Read the Consent</a></div>";
   }
}

async function consent_form_clicked(){
    popup_nofrills('consent.html', 800, 600);
    append_play_link();
}

/*** CALL ME AT END OF TASK ***/
// set completion code and try to submit form
// hopefully triggered by the popup this page opens
function taskCompleteCode(code){
 const codebox =  document.querySelector("input[name='completecode']");
 if(codebox === null){
     console.log("error: cannot find complete code input");
     return(false);
 }
 codebox.value=code;
 codebox.parentElement.submit();
 return(true);
}

// for testing with jest
if (typeof module == 'undefined') { var module = {}; }
module.exports = [add_consent, taskCompleteCode, append_play_link, change_to_play_button, get_anchor, url_params,consent_form_clicked, task_popup_window];
