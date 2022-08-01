// parts of the task we can change with a flag
let tweaks = {'nocaptcha': 'skip audio confirmatin/captcha',
              'where=mri': 'MRI: glove box and embeded up/down survey',
              'noinstruction': 'skip all instructions (go to ready screen)',
              'VERBOSEDEBUG': 'verbose debugging',
              'fewtrials': 'reduce trial count: only 1 pair ea. per block',
              'NO_TIMEOUT': 'disable timeout',
              'photodiode': 'photodiode (sEEG)',
              'nofar': 'make all wells equadistant (no far well)',
              'yesfar': 'far well ~3x as far as close (this used if conflict)',
              'mx95': 'best well prob=95',
              'devalue1': '3 blocks (original 8 versions)',
              'devalue2=75': '4th deval all equal',
              'devalue2=100_80': '4th deval good now bad',
              'norev': 'remove 2nd block reversal',
              'step=slow': 'very slow moving avatar for debuging'
}

let landscapes = ['ocean', 'mountain', 'desert', 'wellcoin']
let timing_choices = ['random', 'debug', 'mrA1', 'mrA2', 'mrB1', 'mrB2', 'quickrandom', 'randomA','randomB']

// grab value of an input
function qs(name) {
  let query = 'input[name=' + name + ']';
  dom = document.querySelector(query);
  return(dom?dom.value:null);
}

// return 4 digit year, 2 digit month, and 2 digit day
// the cononical 8 digit date after lunaid for ld8
function tenpad(n) { return(String(n).padStart(2,'0')); }
function yyyymmdd(){
    d = new Date();
    return (d.getFullYear() + tenpad(d.getMonth()) + tenpad(d.getDay()))
}

// settings to append to anchor of url
function ifchecked(name){
 let dom = document.querySelector("input[name='"+name+"']")
 let box = dom?dom.checked:false;
 return(box?name:"")
}

function get_anchor(){
    let landscape =  document.querySelector("#landscape");
    let timing =  document.querySelector("#timingchoice");
    //selectedOptions[landscape.selectedIndex].value;
    let ltype = "landscape=" + (landscape?landscape.value:"ocean");
    let ttype = "timing="    + (timing?timing.value:"random");
    let tweakstr = Object.keys(tweaks)
                 .map(x=>ifchecked(x))
                 .filter(x=>x!="")
                 .join("&");
    return("#" + ltype + "&" + ttype + (tweakstr?("&"+tweakstr):""))
}

function add_landscape_choices(){
   html  = '<tr><td><label for="landscape">landscape type:</label></td>';
   html += '<td><select id="landscape">';
   landscapes.forEach((x,i)=>
         html +='<option name="'+ x +'"' + (i==0?"selected":"") + '>' + x + '</option>')
   html +=  '</select>  <br>';
   let f =  document.querySelector("#task_setting_tweaks");
   f.innerHTML += html;
}
function add_timing_choices(){
   html = '<td><label for="timingchoice">timing:</label></td><td><select id="timingchoice">'
   timing_choices.forEach((x,i)=>
         html +='<option name="'+ x +'"' + (i==0?"selected":"") + '>' + x + '</option>')
   html +=  '</select>  <br></td></tr>';
   let f =  document.querySelector("#task_setting_tweaks");
   f.innerHTML += html;
}

// put tweak checkboxs in link
function add_tweaks(){
   add_landscape_choices();
   add_timing_choices();

   let f =  document.querySelector("#task_setting_tweaks");
   Object.keys(tweaks).forEach(x =>
         f.innerHTML += ('<tr><td colspan=2><input type="checkbox" name="'+ x +'"/> '+ tweaks[x] +' <br></td></tr>'))
} 
