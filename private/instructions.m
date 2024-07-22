function [onset, output] = instructions(system, number, t)
   output.instruction = number;
   output.msg = 'example output';

   % show the background
   Screen('DrawTexture', system.w, system.tex.ocean_bottom);
   Screen('DrawTexture', system.w, system.tex.chest);

   % run
   onset = Screen('Flip', system.w, t.onset);
end
