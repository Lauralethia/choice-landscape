// parts of the task we can change with a flag
let tweaks = {'nocaptcha': 'skip audio confirmatin/captcha?',
              'where=mri': 'MRI: glove box and embeded up/down survey',
              'noinstruction': 'skip all instructions (go to ready screen)',
              'VERBOSEDEBUG': 'verbose debugging',
              'fewtrials': 'reduce trial count: only 1 pair ea. per block',
              'NO_TIMEOUT': 'disable timeout',
              'photodiode': 'photodiode (sEEG)',
              'nofar': 'make all wells equadistant (no far well)?',
              'yesfar': 'far well ~3x as far as close (this used if conflict)',
              'mx95': 'best well prob=95',
              'devalue1': '3 blocks (original 8 versions)',
              'devalue2=75': '4th deval all equal',
              'devalue2=100_80': '4th deval good now bad',
              'norev': 'remove 2nd block reversal'
}
// settings to append to anchor of url
function ifchecked(name){
 let box =  document.querySelector("input[name='"+name+"']").checked;
 return(box?name:"")
}

function get_anchor(){
    let landscape =  document.querySelector("#landscape")
    let ltype = landscape.selectedOptions[landscape.selectedIndex].value;
    let tweakstr = Object.keys(tweaks)
                 .map(x=>ifchecked(x))
                 .filter(x=>x!="")
                 .join("&");
    return("#" + ltype + (tweakstr?("&"+tweakstr):""))
}

// put tweak checkboxs in link
function add_tweaks(){
   let f =  document.querySelector("#task_setting_tweaks");
   Object.keys(tweaks).forEach(x =>
         f.innerHTML += ('<input type="checkbox" name="'+ x +'"/> '+ tweaks[x] +'? <br>'))
} 
