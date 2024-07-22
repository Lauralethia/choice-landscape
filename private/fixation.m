function [onset, output] = fixation(system, t)

   % show the background
   Screen('DrawTexture', system.w, system.tex.ocean_bottom);
   DrawFormattedText(system.w, '+' ,'center','center', t.cross_color);

   % run
   onset = t.onset + system.starttime;
   onset = Screen('Flip', system.w, onset);
   output = [];
end
